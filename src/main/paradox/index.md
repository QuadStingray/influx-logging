# Influx Logging
This project is an [Logback-Appender](https://logback.qos.ch/) for [InfluxDB](https://www.influxdata.com/get-influxdb/).

## Build Informations
[![Build Status](https://travis-ci.org/QuadStingray/influx-logging.svg?branch=master)](https://travis-ci.org/QuadStingray/influx-logging)
[ ![Download from Bintray](https://api.bintray.com/packages/quadstingray/maven/influx-logging/images/download.svg) ](https://bintray.com/quadstingray/maven/influx-logging/_latestVersion)

## maven
influx-logging is deployed on bintray (jcenter).

## SBT
@@@ vars
@@dependency[sbt,Maven,Gradle] {
  group="com.quadstingray"
  artifact="influx-logging_2.13"
  version="$project.version$"
}
@@@

## Licence
[Apache 2 License.](https://github.com/QuadStingray/influx-logging/blob/master/LICENSE)

## Todos:
- Tests
- Documentation

@@@ index
* [Enabling the plugin](samples/index.md)
@@@