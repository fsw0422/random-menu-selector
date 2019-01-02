name := "RandomMenuSelector"
organization := "com.fsw0422"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.6"

val catsVersion = "1.0.0-RC1"
val catsEffectVersion = "1.0.0"
val monocleVersion = "1.4.0"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  guice,
  "javax.mail" % "mail" % "1.4.7",
  "com.typesafe.slick" %% "slick" % "3.2.3",
  "com.h2database" % "h2" % "1.4.197",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-law" % monocleVersion % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "org.mockito" %% "mockito-scala" % "1.0.0-beta.7" % Test
)

lazy val RandomMenuSelector = (project in file("."))
  .enablePlugins(PlayScala)

fork in run := true
