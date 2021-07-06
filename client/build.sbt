name := "sensor-data-java-grpc-client"

version := "1.0"

scalaVersion := "2.13.2"

lazy val akkaVersion = "2.6.10"
lazy val akkaHttpVersion = "10.2.0"
lazy val akkaGrpcVersion = "1.0.2"
//lazy val jacksonVersion = "2.12.3"

enablePlugins(AkkaGrpcPlugin)

akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
  "com.typesafe.akka" %% "akka-pki" % akkaVersion,

//  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,

// The Akka HTTP overwrites are required because Akka-gRPC depends on 10.1.x
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
//  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.google.protobuf" % "protobuf-java-util" % "3.17.3",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test

)

fork in run := true