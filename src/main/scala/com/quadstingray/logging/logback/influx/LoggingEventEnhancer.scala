package com.quadstingray.logging.logback.influx

import ch.qos.logback.classic.spi.ILoggingEvent
import org.influxdb.dto.Point

trait LoggingEventEnhancer {
  def enhanceLogEntry(builder: Point.Builder, e: ILoggingEvent)
}
