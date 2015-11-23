import sbt.Keys._
import sbt._

trait Dependencies {
  this: Build =>

  val sprayHttp = "io.spray" %% "spray-can" % Versions.spray
  val sprayJson = "io.spray" %%  "spray-json" % Versions.sprayJson
  val sprayRouting = "io.spray" %% "spray-routing" % Versions.spray
  val sprayTestKit = "io.spray" %% "spray-testkit" % Versions.spray % "test" exclude("org.specs2", "specs2_2.11")
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % Versions.akka
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % Versions.akka
  val specs2Core = "org.specs2" %% "specs2-core" % Versions.specs2
  val scalaz = "org.scalaz" %% "scalaz-core" % Versions.scalaz
  val jodaTime = "joda-time" % "joda-time" % Versions.jodaTime

  val baseDepts = Seq(specs2Core % "test")

  val commonDeps = Seq(libraryDependencies ++= baseDepts)

  val apiDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    sprayHttp,
    sprayJson,
    sprayRouting,
    sprayTestKit,
    akkaActor,
    akkaTestKit % "test"))

  val servicesDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    jodaTime,
    scalaz,
    sprayJson))

  val systemDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(scalaz))
}
