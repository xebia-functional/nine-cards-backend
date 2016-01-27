import sbt.Keys._
import sbt._

trait Dependencies {
  this: Build =>

  val sprayHttp = "io.spray" %% "spray-can" % Versions.spray
  val sprayJson = "io.spray" %% "spray-json" % Versions.sprayJson
  val sprayRouting = "io.spray" %% "spray-routing-shapeless2" % Versions.spray
  val sprayTestKit = "io.spray" %% "spray-testkit" % Versions.spray % "test" exclude("org.specs2", "specs2_2.11")
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % Versions.akka
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % Versions.akka
  val cats = "org.spire-math" %% "cats" % Versions.cats
  val specs2Core = "org.specs2" %% "specs2-core" % Versions.specs2
  val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % Versions.specs2
  val scalaz = "org.scalaz" %% "scalaz-core" % Versions.scalaz
  val scalazConcurrent = "org.scalaz" %% "scalaz-concurrent" % Versions.scalaz
  val jodaTime = "joda-time" % "joda-time" % Versions.jodaTime
  val doobieCore = "org.tpolecat" %% "doobie-core" % Versions.doobie
  val doobieH2 = "org.tpolecat" %% "doobie-contrib-h2" % Versions.doobie
  val doobiePostgresql = "org.tpolecat" %% "doobie-contrib-postgresql" % Versions.doobie
  val doobieSpecs2 = "org.tpolecat" %% "doobie-contrib-specs2" % Versions.doobie

  val baseDepts = Seq(
    specs2Core % "test" exclude("org.scalaz", "*"),
    specs2Scalacheck % "test")

  val apiDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    sprayHttp,
    sprayJson,
    sprayRouting,
    sprayTestKit,
    scalaz,
    scalazConcurrent,
    akkaActor,
    akkaTestKit % "test"))

  val commonDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    scalaz,
    scalazConcurrent))

  val processesDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    scalaz,
    scalazConcurrent))

  val servicesDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    jodaTime,
    cats,
    doobieCore exclude("org.scalaz", "*"),
    doobieH2,
    doobiePostgresql,
    doobieSpecs2 % "test",
    scalaz,
    scalazConcurrent,
    sprayJson))
}
