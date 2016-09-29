import sbt.Keys._
import sbt._

object Dependencies {

  def akka(suff: String) = "com.typesafe.akka" %% "akka-actor" % Versions.akka
  val akkaActor = akka("-actor")
  val akkaTestKit = akka("-testkit")

  def spray(suff: String) = "io.spray" %% s"spray${suff}" % Versions.spray

  val sprayTestKit = spray("-testkit") % "test" exclude("org.specs2", "specs2_2.11")

  val sprayJson = "io.spray" %% "spray-json" % Versions.sprayJson

  val cats = "org.typelevel" %% "cats" % Versions.cats

  def specs2(suff: String) = "org.specs2" %% s"specs2${suff}" % Versions.specs2 % "test"
  val specs2Core = specs2("-core") exclude("org.scalaz", "*")

  def scalaz(suff: String) = "org.scalaz" %% s"scalaz${suff}" % Versions.scalaz

  val jodaConvert = "org.joda" % "joda-convert" % Versions.jodaConvert
  val jodaTime = "joda-time" % "joda-time" % Versions.jodaTime

  def doobie(suff: String) = "org.tpolecat" %% s"doobie$suff" % Versions.doobie exclude("org.scalaz", "*")
  val typesafeConfig = "com.typesafe" % "config" % Versions.typesafeConfig
  val flywaydbCore = "org.flywaydb" % "flyway-core" % Versions.flywaydb

  val scalacheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % Versions.scalacheckShapeless

  def http4s(suff: String) = "org.http4s" %% s"http4s${suff}" % Versions.http4s

  def circe(suff: String) = "io.circe" %% s"circe$suff" % Versions.circe
  val mockserver = "org.mock-server" % "mockserver-netty" % Versions.mockserver
  val hasher = "com.roundeights" %% "hasher" % Versions.hasher
  val newRelic = "com.newrelic.agent.java" % "newrelic-agent" % Versions.newRelic
  def enumeratum(suffix: String) = "com.beachape" %% s"enumeratum$suffix" % Versions.enumeratum

  val baseDepts = Seq(
    hasher,
    scalacheckShapeless % "test",
    scalaz("-concurrent"),
    scalaz("-core"),
    specs2("-cats"),
    specs2Core, 
    specs2("-mock"),
    specs2("-scalacheck"),
    typesafeConfig
  )

  val apiDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    akkaActor,
    akkaTestKit % "test",
    cats % "test",
    circe("-core"),
    circe("-spray"),
    newRelic,
    spray("-can"),
    spray("-routing-shapeless2"),
    sprayJson,
    sprayTestKit
  ))

  val processesDeps = Seq(libraryDependencies ++= baseDepts)

  val servicesDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    cats,
    circe("-core"),
    circe("-generic"),
    doobie("-contrib-hikari"),
    doobie("-contrib-postgresql"),
    doobie("-contrib-h2") % "test",
    doobie("-contrib-specs2") % "test",
    doobie("-core"),
    enumeratum(""),
    enumeratum("-circe"),
    flywaydbCore % "test",
    http4s("-blaze-client"),
    http4s("-circe"),
    jodaConvert,
    jodaTime,
    mockserver % "test",
    sprayJson
  ))
}
