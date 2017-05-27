name := """traffic-stress-server"""
organization := "com.stressrelievers"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.11"
routesGenerator := InjectedRoutesGenerator

libraryDependencies += filters
libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test,
  "org.reactivemongo" % "play2-reactivemongo_2.11" % "0.12.3"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.stressrelievers.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.stressrelievers.binders._"
