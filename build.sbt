name := "soa-simulation"
version := "1.0"

scalaVersion := "2.12.6"
lazy val akkaV = "2.5.21"

enablePlugins(GatlingPlugin)

val gatlingV = "3.0.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"        % "10.1.7",
  "com.typesafe.akka" %% "akka-stream"      % akkaV,
  "com.typesafe.akka" %% "akka-actor"       % akkaV,
  "de.heikoseeberger" %% "akka-http-json4s" % "1.25.2",
  "org.json4s"        %% "json4s-native"    % "3.6.5",

  "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingV % "test",
  "io.gatling"            % "gatling-test-framework"    % gatlingV % "test"
)

mainClass in assembly := Some("com.example.Server")

enablePlugins(DockerPlugin)

imageNames in docker := Seq(
  ImageName(s"crankydillo/${name.value}:latest")
)

dockerfile in docker := {
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("openjdk:8-jre")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}
