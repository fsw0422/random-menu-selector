name := """RandomMenuSelector"""
organization := "com.fsw0422"

version := "1.0.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.12.6"

val catsVersion = "1.0.0-RC1"
val monocleVersion = "1.4.0"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-macros" % catsVersion,
  "org.typelevel" %% "cats-kernel" % catsVersion,
  "org.typelevel" %% "cats-free" % catsVersion,
  "org.typelevel" %% "cats-effect" % "0.5",
  "org.typelevel" %% "cats-effect-laws" % "0.5" % Test,
  "org.typelevel" %% "cats-laws" % catsVersion % Test,
  "org.typelevel" %% "cats-testkit" % catsVersion % Test,
  "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-law" % monocleVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
