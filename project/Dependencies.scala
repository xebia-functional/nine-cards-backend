
object Dependencies {

  import sbt._

  private def akka(suff: String) = "com.typesafe.akka" %% s"akka${suff}" % Versions.akka
  private def circe(suff: String) = "io.circe" %% s"circe${suff}" % Versions.circe
  private def enumeratum(suff: String) = "com.beachape" %% s"enumeratum${suff}" % Versions.enumeratum
  private def specs2(suff: String) = "org.specs2" %% s"specs2${suff}" % Versions.specs2 % "test"
  private def spray(suff: String) = "io.spray" %% s"spray${suff}" % Versions.spray

  private val akkaActor = "com.typesafe.akka" %% "akka-actor" % Versions.akka

  private val cats = "org.typelevel" %% "cats" % Versions.cats
  private val embeddedRedis = "com.orange.redis-embedded" % "embedded-redis" % Versions.embeddedRedis % "test"
  private val http4sClient = "org.http4s" %% "http4s-blaze-client" % Versions.http4s
  private val jodaConvert = "org.joda" % "joda-convert" % Versions.jodaConvert
  private val jodaTime = "joda-time" % "joda-time" % Versions.jodaTime
  private val mockserver = "org.mock-server" % "mockserver-netty" % Versions.mockserver
  private val newRelic = "com.newrelic.agent.java" % "newrelic-agent" % Versions.newRelic
  private val redisClient = "net.debasishg" %% "redisclient" % Versions.redisClient
  private val scalaCheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % Versions.scalacheckShapeless % "test"
  private val tagSoup = "org.ccil.cowan.tagsoup" % "tagsoup" % Versions.tagSoup

  private val specs2Core = specs2("-core") exclude("org.scalaz", "*")
  private val sprayTestKit = spray("-testkit") % "test" exclude("org.specs2", "specs2_2.11")

  val googleplayDeps = Seq(sbt.Keys.libraryDependencies ++= Seq(
    akka("-actor"),
    cats,
    circe("-core"),
    circe("-generic"),
    circe("-parser"),
    circe("-spray"),
    embeddedRedis,
    enumeratum(""),
    enumeratum("-circe"),
    http4sClient,
    jodaConvert,
    jodaTime,
    mockserver,
    newRelic,
    redisClient,
    scalaCheckShapeless,
    specs2Core,
    specs2("-matcher-extra"),
    specs2("-mock"),
    specs2("-scalacheck"),
    spray("-can"),
    spray("-routing-shapeless2"),
    sprayTestKit,
    tagSoup
  ))
}
