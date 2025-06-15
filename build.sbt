import scala.collection.immutable.Seq

name := """music-service"""
organization := "test.task"

version := "1.0-SNAPSHOT"

scalaVersion := "2.13.16"

libraryDependencies ++= Seq(
  guice,
  filters,
  "ch.qos.logback" % "logback-classic" % "1.5.18",
  "ch.qos.logback" % "logback-core" % "1.5.18",
  "org.sangria-graphql" % "sangria_2.13" % "4.2.5",
  "org.sangria-graphql" % "sangria-slowlog_2.13" % "3.0.0",
  "org.sangria-graphql" % "sangria-play-json-play30_2.13" % "2.0.3",

  "com.typesafe.slick" %% "slick" % "3.5.2",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.5.2",

  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.5.2",
  "org.postgresql" % "postgresql" % "42.7.7",

  "org.scalatest" %% "scalatest" % "3.2.19" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)


// Adds additional packages into Twirl
//TwirlKeys.templateImports += "test.task.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "test.task.binders._"
