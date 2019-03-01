name := "soa-simulation"
version := "1.0"

scalaVersion := "2.12.6"
lazy val akkaVersion = "2.5.21"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"        % "10.1.7",
  "com.typesafe.akka" %% "akka-stream"      % akkaVersion,
  "com.typesafe.akka" %% "akka-actor"       % akkaVersion,
  "de.heikoseeberger" %% "akka-http-json4s" % "1.25.2",
  "org.json4s"        %% "json4s-native"    % "3.6.5"
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
