name := "random-menu-selector"
organization := "com.fsw0422"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.8"

resolvers += Resolver.jcenterRepo

dockerRepository := Some("fsw0422")

val monocleVersion = "1.4.0"

libraryDependencies ++= Seq(
  guice,
  "javax.mail" % "mail" % "1.4.7",
  "com.typesafe.slick" %% "slick" % "3.2.3",
  "com.h2database" % "h2" % "1.4.197",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-law" % monocleVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.mockito" %% "mockito-scala" % "1.0.0-beta.7" % Test
)

lazy val RandomMenuSelector = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(DockerPlugin)
  .enablePlugins(ClasspathJarPlugin)

fork in run := true
fork in test := true
