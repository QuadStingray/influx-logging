package com.quadstingray.logging.logback.influx

import java.util.concurrent.TimeUnit

import ch.qos.logback.classic.spi.{ILoggingEvent, IThrowableProxy}
import ch.qos.logback.classic.{ClassicConstants, Level, LoggerContext}
import ch.qos.logback.core.UnsynchronizedAppenderBase
import ch.qos.logback.core.boolex.EvaluationException
import ch.qos.logback.core.util.Loader
import com.quadstingray.logging.logback.influx.LoggingAppender._
import org.influxdb.dto.{Point, Query}
import org.influxdb.{BatchOptions, InfluxDB, InfluxDBFactory}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization
import org.slf4j
import org.slf4j.LoggerFactory

import scala.beans.BeanProperty
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


class LoggingAppender extends UnsynchronizedAppenderBase[ILoggingEvent] {
  val logger: slf4j.Logger = LoggerFactory.getLogger(classOf[LoggingAppender])

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  private var influxDB: InfluxDB = _

  private var loggingEventEnhancers: ArrayBuffer[LoggingEventEnhancer] = _

  private val loggingEventEnhancerClassNames: ArrayBuffer[String] = ArrayBuffer()

  @BeanProperty
  var host: String = ""

  @BeanProperty
  var userName: String = _

  @BeanProperty
  var password: String = ""

  @BeanProperty
  var dbName: String = "influx-logging"

  @BeanProperty
  var flushLevel: Level = Level.ERROR

  @BeanProperty
  var measurementName: String = "influx-logging"

  @BeanProperty
  var version: String = ""

  @BeanProperty
  var includeCallerData: Boolean = false

  @BeanProperty
  var maxCallerDataDepth: Int = ClassicConstants.DEFAULT_MAX_CALLEDER_DATA_DEPTH

  @BeanProperty
  var retentionPolicyName: String = "InfluxLoggingAppenderPolicy"

  @BeanProperty
  var retentionPolicyDuration: String = "INF"

  @BeanProperty
  var retentionPolicyReplication: String = "1"

  @BeanProperty
  var retentionPolicyCreationString: String = ""

  def addLoggingEventEnhancer(enhancerClassName: String): Unit = {
    this.loggingEventEnhancerClassNames.+=(enhancerClassName)
  }

  private[logback] def getLoggingEventEnhancers: ArrayBuffer[LoggingEventEnhancer] = getEnhancers(loggingEventEnhancerClassNames)

  private[logback] def getEnhancers[T](classNames: ArrayBuffer[String]): ArrayBuffer[T] = {
    val loggingEnhancers = ArrayBuffer[T]()
    if (classNames != null) {
      for (enhancerClassName <- classNames) {
        if (enhancerClassName != null) {
          val enhancer = getEnhancer(enhancerClassName)
          if (enhancer.isDefined)
            loggingEnhancers.+=(enhancer.get)
        }
      }
    }
    loggingEnhancers
  }

  private def getEnhancer[T](enhancerClassName: String): scala.Option[T] = {
    try {
      val clz = Loader.loadClass(enhancerClassName.trim).asInstanceOf[Class[T]]
      Some(clz.newInstance)
    } catch {
      case _: Exception =>
        None
    }
  }

  override def start(): Unit = {
    if (isStarted)
      return

    if (userName.trim != "")
      influxDB = InfluxDBFactory.connect(host, userName, password)
    else
      influxDB = InfluxDBFactory.connect(host)

    if (!influxDB.ping().isGood) {
      throw new EvaluationException("Could not check InfluxDB Connection (Host: %s | UserName: %s | Password: %s)")
    }

    influxDB.enableGzip()
    influxDB.enableBatch(BatchOptions.DEFAULTS)

    influxDB.query(new Query("CREATE DATABASE " + dbName))
    influxDB.setDatabase(dbName)

    createRetentionPolicy()

    context match {
      case loggerContext: LoggerContext => loggerContext.setMaxCallerDataDepth(maxCallerDataDepth)
      case _ =>
    }

    loggingEventEnhancers = ArrayBuffer[LoggingEventEnhancer]()
    loggingEventEnhancers.++=(getLoggingEventEnhancers)
    super.start()
  }

  private def createRetentionPolicy(): Unit = {
    if (retentionPolicyCreationString.trim == "")
      retentionPolicyCreationString = "CREATE RETENTION POLICY \"" + retentionPolicyName + "\" ON \"" + dbName + "\" DURATION INF REPLICATION " + retentionPolicyReplication + " SHARD DURATION 30m DEFAULT"

    val createRetentionPolicyResult = influxDB.query(new Query(retentionPolicyCreationString))

    if (createRetentionPolicyResult.hasError)
      retentionPolicyCreationString = retentionPolicyCreationString.toUpperCase().replace("CREATE RETENTION POLICY", "ALTER RETENTION POLICY")

    val alterRetentionPolicyResult = influxDB.query(new Query(retentionPolicyCreationString))

    if (alterRetentionPolicyResult.hasError && createRetentionPolicyResult.hasError) {
      throw new EvaluationException("Could not create or update rentationPolicy (CreateError: %s | AlterError: %s)".format(createRetentionPolicyResult.getError, alterRetentionPolicyResult.getError))
    }

    influxDB.setRetentionPolicy(retentionPolicyName)
  }


  override protected def append(e: ILoggingEvent): Unit = {
    val logEntry = logEntryFor(e)
    influxDB.write(logEntry)
    if (flushLevel.levelInt >= e.getLevel.levelInt) {
      influxDB.flush()
    }
  }

  override def stop(): Unit = {
    influxDB.flush()
    influxDB.close()
    super.stop()
  }

  private def logEntryFor(e: ILoggingEvent): Point = {

    val measurement = Point.measurement(measurementName).time(System.currentTimeMillis, TimeUnit.MILLISECONDS)

    measurement.tag(LogLevelString, e.getLevel.levelStr)
    measurement.tag(LoggerName, e.getLoggerName)

    measurement.addField(LogLevelValue, e.getLevel.levelInt)
    measurement.addField(FormattedLogMessage, e.getFormattedMessage)
    measurement.addField(LogMessage, e.getMessage)
    measurement.addField(CallerData, Serialization.write(e.getCallerData.map(_.toString).toList))

    if (version.trim != "") {
      measurement.addField(Version, version)
    }

    if (e.getThrowableProxy != null) {
      val proxyMap = getThrowableProxyMap(e.getThrowableProxy)
      proxyMap.foreach(element => {
        measurement.addField(element._1, Serialization.write(element._2))
      })
    }

    if (loggingEventEnhancers != null) {
      for (enhancer <- loggingEventEnhancers) {
        enhancer.enhanceLogEntry(measurement, e)
      }
    }
    measurement.build
  }


  private def getThrowableProxyMap(proxy: IThrowableProxy): Map[String, AnyRef] = {
    val proxyInfos = mutable.Map[String, AnyRef]()
    if (proxy != null) {
      proxyInfos.put(ExceptionClassName, proxy.getClassName)
      proxyInfos.put(StackTrace, proxy.getStackTraceElementProxyArray.toList.splitAt(proxy.getCommonFrames)._1.map(_.toString))
      if (proxy.getCause != null) {
        proxyInfos.put(CausedBy, getThrowableProxyMap(proxy.getCause))
      }
    }
    proxyInfos.toMap
  }

}


object LoggingAppender {
  val ExceptionClassName = "exceptionClassName"
  val StackTrace = "stackTrace"
  val CausedBy = "causedBy"
  val LogLevelString = "level"
  val LogLevelValue = "levelValue"
  val FormattedLogMessage = "formattedMessage"
  val LogMessage = "message"
  val CallerData = "callerData"
  val LoggerName = "loggerName"
  val Version = "version"
}
