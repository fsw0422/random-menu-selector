name := "RandomMenuSelector"
organization := "com.fsw0422"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.6"

val catsVersion = "1.0.0-RC1"
val catsEffectVersion = "1.0.0"
val monocleVersion = "1.4.0"

libraryDependencies ++= Seq(
  guice,
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-macros" % catsVersion,
  "org.typelevel" %% "cats-kernel" % catsVersion,
  "org.typelevel" %% "cats-free" % catsVersion,
  "org.typelevel" %% "cats-testkit" % catsVersion % Test,
  "org.typelevel" %% "cats-laws" % catsVersion % Test,
  "org.typelevel" %% "cats-effect" % catsEffectVersion,
  "org.typelevel" %% "cats-effect-laws" % catsEffectVersion % Test,
  "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-law" % monocleVersion % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)

lazy val RandomMenuSelector = (project in file("."))
  .enablePlugins(PlayScala)
