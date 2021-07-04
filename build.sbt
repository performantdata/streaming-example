name    := "streaming-example"
version := "0.1.0"
scalaVersion := "3.0.0"
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
)

val circeVersion = "0.14.1"
val http4sVersion = "1.0.0-M23"
val jacksonVersion = "2.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core"    % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser"  % circeVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-circe"        % http4sVersion,
  "org.http4s" %% "http4s-dsl"          % http4sVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",

  // Log4J2 runtime
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.14.1",
  "com.fasterxml.jackson.core"       % "jackson-databind"        % jacksonVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion,

  "org.scalatest" %% "scalatest-freespec" % "3.2.9" % Test,
)

// Docker packaging
enablePlugins(JavaAppPackaging, DockerPlugin)
dockerBaseImage    := "circleci/openjdk:11"
dockerEnvVars      := Map("PATH" -> ((Docker / defaultLinuxInstallLocation).value + "/bin:${PATH}"))
dockerExposedPorts := Seq(8080)
dockerUpdateLatest := true
