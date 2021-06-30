
import sbt.{ThisBuild, _}
import sbt.Keys.{watchSources, _}

ThisBuild / version := "0.0.1"

val credentialFile = new File("lightbend.sbt")

def doesCredentialExist : Boolean = {
  import java.nio.file.Files
  val exists = Files.exists(credentialFile.toPath)
  println(s"doesCredentialExist: ($credentialFile) " + exists)
  exists
}

def commercialDependencies : Seq[ModuleID] = {
  import com.lightbend.cinnamon.sbt.Cinnamon.CinnamonKeys.cinnamon
  Seq(
    // BEGIN: this requires a commercial Lightbend Subscription
    Cinnamon.library.cinnamonAgent,
    Cinnamon.library.cinnamonAkka,
    Cinnamon.library.cinnamonAkkaStream,
    Cinnamon.library.cinnamonAkkaHttp,
    Cinnamon.library.cinnamonPrometheus,
    Cinnamon.library.cinnamonPrometheusHttpServer,
    Cinnamon.library.cinnamonJvmMetricsProducer,
/*
    Cinnamon.library.cinnamonOpenTracing,
    Cinnamon.library.cinnamonOpenTracingJaeger
*/
    // END: this requires a commercial Lightbend Subscription
  )
}

def ossDependencies : Seq[ModuleID] = {
  Seq(
    "com.lightbend.akka"     %% "akka-stream-alpakka-file"  % "1.1.2",
    "com.thesamet.scalapb"   %% "scalapb-json4s"            % "0.10.1",
    "ch.qos.logback"         %  "logback-classic"           % "1.2.3",
    "com.typesafe.akka"      %% "akka-http-testkit"         % "10.1.12" % "test",
    "org.scalatest"          %% "scalatest"                 % "3.0.8"  % "test"
  )
}

lazy val sensorData =  (project in file("."))
  .enablePlugins(CloudflowApplicationPlugin, CloudflowAkkaPlugin)
  .enablePlugins(CloudflowLibraryPlugin)
  .enablePlugins(Cinnamon)
  .settings(
    scalaVersion := "2.12.12",

    schemaCodeGenerator := SchemaCodeGenerator.Java,

    runLocalConfigFile := Some("src/main/resources/local.conf"),
    runLocalLog4jConfigFile := Some("src/main/resources/log4j.xml"),

    name := "sensor-data-java-proto-grpc",

    // Add the Cinnamon Agent settings for run and test
    cinnamonSuppressRepoWarnings := true,
    test / cinnamon := false,
    run / cinnamon := true,
    cinnamonLogLevel := "INFO",

    libraryDependencies ++= {
      if (doesCredentialExist) {
        commercialDependencies ++ ossDependencies
      }
      else {
        ossDependencies
      }
    },
    javacOptions += "-Xlint:deprecation",
    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-target:jvm-1.8",
      "-Xlog-reflective-calls",
      "-Xlint",
      "-Ywarn-unused",
      "-Ywarn-unused-import",
      "-deprecation",
      "-feature",
      "-language:_",
      "-unchecked"
    ),
    organization := "com.lightbend.cloudflow",
    headerLicense := Some(HeaderLicense.ALv2("(C) 2016-2021", "Lightbend Inc. <https://www.lightbend.com>")),

  )

