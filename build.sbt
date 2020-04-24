name := "influx-logging"

organization := "com.quadstingray"

crossPaths := true

scalaVersion := crossScalaVersions.value.last

crossScalaVersions := List("2.12.11", "2.13.1", "2.13.2")

scalacOptions := Seq("-unchecked", "-deprecation", "-Ywarn-unused", "-Yrangepos")

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % Provided

libraryDependencies ++= Seq(
  "org.influxdb" % "influxdb-java" % "2.18"
)

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.7"

// Tests
libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "4.9.3" % Test,
  "junit" % "junit" % "4.13" % Test
)

homepage := Some(url("https://quadstingray.github.io/influx-logging/"))

scmInfo := Some(ScmInfo(url("https://github.com/QuadStingray/influx-logging"), "https://github.com/QuadStingray/influx-logging.git"))

developers := List(Developer("QuadStingray", "QuadStingray", "github@quadstingray.com", url("https://github.com/QuadStingray")))

licenses += ("Apache-2.0", url("https://github.com/QuadStingray/influx-logging/blob/master/LICENSE"))

resolvers += Resolver.jcenterRepo

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.jcenterRepo

bintrayReleaseOnPublish in ThisBuild := true

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies, // : ReleaseStep
  inquireVersions, // : ReleaseStep
  setReleaseVersion, // : ReleaseStep
  commitReleaseVersion, // : ReleaseStep, performs the initial git checks
  tagRelease, // : ReleaseStep
  setNextVersion, // : ReleaseStep
  commitNextVersion, // : ReleaseStep
  pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
)
