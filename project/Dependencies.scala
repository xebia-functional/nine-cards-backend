import sbt.Keys._
import sbt._

trait Dependencies {
  this: Build =>

  val sprayHttp = "io.spray" %% "spray-can" % Versions.spray
  val sprayJson = "io.spray" %%  "spray-json" % Versions.sprayJson
  val sprayRouting = "io.spray" %% "spray-routing" % Versions.spray
  val sprayTestKit = "io.spray" %% "spray-testkit" % Versions.spray
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % Versions.akka
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % Versions.akka
  val specs2Core = "org.specs2" %% "specs2-core" % Versions.specs2
  val scalaz = "org.scalaz" %% "scalaz-core" % Versions.scalaz

  val baseDepts = Seq(specs2Core % "test")

  val commonDeps = Seq(libraryDependencies ++= baseDepts)

  val appDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    scalaz,
    sprayHttp,
    sprayRouting))

  val servicesDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(scalaz))

  val userDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    akkaTestKit % "test"))

  val apiDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    sprayHttp,
    sprayJson,
    sprayRouting,
    sprayTestKit,
    akkaActor,
    akkaTestKit % "test"))
}