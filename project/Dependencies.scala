
object Dependencies {

  import sbt._
  import sbt.Keys.libraryDependencies

  private def akka(suff: String) = "com.typesafe.akka" %% s"akka${suff}" % Versions.akka
  private def circe(suff: String) = "io.circe" %% s"circe${suff}" % Versions.circe
  private def doobie(suff: String) = "org.tpolecat" %% s"doobie$suff" % Versions.doobie exclude("org.scalaz", "*")
  private def enumeratum(suff: String) = "com.beachape" %% s"enumeratum${suff}" % Versions.enumeratum
  private def http4s(suff: String) = "org.http4s" %% s"http4s${suff}" % Versions.http4s
  private def scalaz(suff: String) = "org.scalaz" %% s"scalaz${suff}" % Versions.scalaz
  private def specs2(suff: String) = "org.specs2" %% s"specs2${suff}" % Versions.specs2 % "test"
  private def spray(suff: String) = "io.spray" %% s"spray${suff}" % Versions.spray

  private val akkaActor = akka("-actor")
  private val akkaTestKit = akka("-testkit")
  private val cats = "org.typelevel" %% "cats" % Versions.cats
  private val embeddedRedis = "com.orange.redis-embedded" % "embedded-redis" % Versions.embeddedRedis % "test"
  private val flywaydbCore = "org.flywaydb" % "flyway-core" % Versions.flywaydb
  private val hasher = "com.roundeights" %% "hasher" % Versions.hasher
  private val http4sClient = "org.http4s" %% "http4s-blaze-client" % Versions.http4s
  private val jodaConvert = "org.joda" % "joda-convert" % Versions.jodaConvert
  private val jodaTime = "joda-time" % "joda-time" % Versions.jodaTime
  private val mockserver = "org.mock-server" % "mockserver-netty" % Versions.mockserver
  private val newRelic = "com.newrelic.agent.java" % "newrelic-agent" % Versions.newRelic
  private val redisClient = "net.debasishg" %% "redisclient" % Versions.redisClient
  private val scalacheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % Versions.scalacheckShapeless % "test"
  private val specs2Core = specs2("-core") exclude("org.scalaz", "*")
  private val sprayJson = "io.spray" %% "spray-json" % Versions.sprayJson
  private val sprayTestKit = spray("-testkit") % "test" exclude("org.specs2", "specs2_2.11")
  private val tagSoup = "org.ccil.cowan.tagsoup" % "tagsoup" % Versions.tagSoup
  private val typesafeConfig = "com.typesafe" % "config" % Versions.typesafeConfig

  val baseDepts = Seq(
    hasher,
    scalacheckShapeless,
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
    doobie("-contrib-h2"),
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
    scalacheckShapeless,
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
