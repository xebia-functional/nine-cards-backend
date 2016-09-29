import sbt.Keys._
import sbt._

object Dependencies {

  private def spray(name: String) = "io.spray" %% s"spray-${name}" % Versions.spray

  private val sprayHttp = spray("can")
  private val sprayRouting = spray("routing-shapeless2")
  private val sprayTestKit = spray("testkit") % "test" exclude("org.specs2", "specs2_2.11")

  private def specs(name: String) = "org.specs2" %% s"specs2-${name}" % Versions.specs2

  private val specs2Core = specs("core") % "test" exclude("org.scalaz", "*")
  private val specs2MatcherExtra = specs("matcher-extra") % "test"
  private val specs2ScalaCheck = specs("scalacheck") % "test"
  private val specs2Mockito = specs("mock") % "test"

  private val scalaCheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % Versions.scalacheckShapeless % "test"

  private def circe(name: String) = "io.circe" %% s"circe-${name}" % Versions.circe

  private val akkaActor = "com.typesafe.akka" %% "akka-actor" % Versions.akka

  private val cats = "org.typelevel" %% "cats" % Versions.cats

  private val embeddedRedis = "com.orange.redis-embedded" % "embedded-redis" % Versions.embeddedRedis % "test"

  private val http4sClient = "org.http4s" %% "http4s-blaze-client" % Versions.http4s

  private val jodaTime = "joda-time" % "joda-time" % Versions.jodaTime
  private val jodaConvert = "org.joda" % "joda-convert" % Versions.jodaConvert

  private val redisClient = "net.debasishg" %% "redisclient" % Versions.redisClient

  private val tagSoup = "org.ccil.cowan.tagsoup" % "tagsoup" % Versions.tagSoup

  private val newRelic = "com.newrelic.agent.java" % "newrelic-agent" % Versions.newRelic
  private val baseDepts = Seq(specs2Core)

  private val mockserver = "org.mock-server" % "mockserver-netty" % Versions.mockserver

  def enumeratum(suffix: String) = "com.beachape" %% s"enumeratum$suffix" % Versions.enumeratum

  val apiDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    cats,
    sprayHttp,
    sprayRouting,
    sprayTestKit,
    akkaActor,
    http4sClient,
    circe("core"),
    circe("generic"),
    circe("parser"),
    circe("spray"),
    enumeratum(""),
    enumeratum("-circe"),
    jodaTime,
    jodaConvert,
    redisClient,
    embeddedRedis,
    mockserver,
    newRelic,
    scalaCheckShapeless,
    specs2ScalaCheck,
    specs2MatcherExtra,
    specs2Mockito,
    tagSoup))
}
