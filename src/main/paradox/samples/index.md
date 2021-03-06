# Enable Influx Logging

## Add Cloud Logging Dependency to your Project
@@@ vars
@@dependency[sbt,Maven,Gradle] {
  group="com.quadstingray"
  artifact="influx-logging"
  version="$project.version$"
}
@@@

## Add Logback Dependency

@@dependency[sbt,Maven,Gradle] {
  group="ch.qos.logback"
  artifact="logback-classic"
  version="1.2.3"
}

## Add your Logback.xml
Samples:
```xml
<appender name="INFLUX" class="com.quadstingray.logging.logback.influx.LoggingAppender">
    <host>WARN</host> <!-- Required  -->
    <userName>user</userName> <!-- Optional : default empty -->
    <password>user1234</password> <!-- Optional : default empty -->
    <dbName>logging</dbName> <!-- Optional : default "influx-logging" -->
    <measurementName>influx-app</measurementName> <!-- Optional : default "influx-logging" -->
    <flushLevel>WARN</flushLevel> <!-- Optional : default ERROR -->
</appender>
```
Todo: More Samples with

## Modifiy The Log Entry

### Add Setting To Logback.xml
```xml
        <loggingEventEnhancer>com.quadstingray.influx.SpecialEnhancer</loggingEventEnhancer>
```

```scala
import com.quadstingray.logging.logback.influx.LoggingEventEnhancer
import ch.qos.logback.classic.spi.ILoggingEvent

class SpecialEnhancer extends LoggingEventEnhancer {
  override def enhanceLogEntry(logEntry: Point.Builder, e: ILoggingEvent): Unit = {
            logEntry.addField("duration", event.duration)
            logEntry.tag("eventType", "RequestFinishedLogEvent")
  }


}
```