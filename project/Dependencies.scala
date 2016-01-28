import sbt.Keys._
import sbt._

trait Dependencies {
  this: Build =>

  val sprayHttp = "io.spray" %% "spray-can" % Versions.spray
  val sprayRouting = "io.spray" %% "spray-routing-shapeless2" % Versions.spray
  val sprayTestKit = "io.spray" %% "spray-testkit" % Versions.spray % "test" exclude("org.specs2", "specs2_2.11")
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % Versions.akka

  val apiDeps = Seq(libraryDependencies ++= Seq(
    sprayHttp,
    sprayRouting,
    akkaActor))
}
