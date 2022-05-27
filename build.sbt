name := "opdefx"

version := "0.1"

scalaVersion := "2.13.6"

val zioVersion = "1.0.12"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % zioVersion,
  "org.scala-lang.modules" %% "scala-swing" % "2.1.1",
  "org.apache.httpcomponents" % "httpclient" % "4.5.13",
  "org.apache.httpcomponents" % "httpmime" % "4.5.13",
  "org.apache.commons" % "commons-lang3" % "3.12.0",
  "commons-codec" % "commons-codec" % "1.15",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.0",
  "joda-time" % "joda-time" % "2.10.13",
  "org.joda" % "joda-convert" % "2.2.2",
  "com.h2database" % "h2" % "2.1.210",
  "org.scalatest" %% "scalatest-funsuite" % "3.2.10" % Test,
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

//mainClass in (Compile, packageBin) := Some("com.ipto.zopde.ZOPDE")
//mainClass in (Compile, run) := Some("com.ipto.zopde.ZOPDE")