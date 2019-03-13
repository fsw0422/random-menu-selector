name := "random-menu-selector"
organization := "com.fsw0422"

version := "latest"

scalaVersion := "2.12.8"

resolvers += Resolver.jcenterRepo

dockerRepository := Some("fsw0422")

scapegoatVersion in ThisBuild := "1.3.8"

val monocleVersion = "1.4.0"
val slickVersion = "3.2.3"
val slickPgVersion = "0.17.0"

libraryDependencies ++= Seq(
  guice,
  "javax.mail" % "mail" % "1.4.7",
  "org.postgresql" % "postgresql" % "42.1.4",
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "com.github.tminglei" %% "slick-pg" % slickPgVersion,
  "com.github.tminglei" %% "slick-pg_joda-time" % slickPgVersion,
  "com.github.tminglei" %% "slick-pg_play-json" % slickPgVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.mockito" %% "mockito-scala" % "1.0.0-beta.7" % Test
)

lazy val RandomMenuSelector = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(DockerPlugin)
  .enablePlugins(ClasspathJarPlugin)

fork in run := true
fork in test := true
