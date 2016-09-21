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
  val cats = "org.typelevel" %% "cats" % Versions.cats
  val specs2Core = "org.specs2" %% "specs2-core" % Versions.specs2
  val specs2Cats = "org.specs2" %% "specs2-cats" % Versions.specs2
  val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % Versions.specs2
  val specs2Mockito = "org.specs2" %% "specs2-mock" % Versions.specs2
  val scalaz = "org.scalaz" %% "scalaz-core" % Versions.scalaz
  val scalazConcurrent = "org.scalaz" %% "scalaz-concurrent" % Versions.scalaz
  val jodaConvert = "org.joda" % "joda-convert" % Versions.jodaConvert
  val jodaTime = "joda-time" % "joda-time" % Versions.jodaTime
  def doobie(suff: String) = "org.tpolecat" %% s"doobie$suff" % Versions.doobie exclude("org.scalaz", "*")
  val typesafeConfig = "com.typesafe" % "config" % Versions.typesafeConfig
  val flywaydbCore = "org.flywaydb" % "flyway-core" % Versions.flywaydb
  val scalacheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % Versions.scalacheckShapeless
  val http4sClient = "org.http4s" %% "http4s-blaze-client" % Versions.http4s
  val http4sCirce = "org.http4s" %% "http4s-circe" % Versions.http4s
  def circe(suff: String) = "io.circe" %% s"circe$suff" % Versions.circe
  val mockserver = "org.mock-server" % "mockserver-netty" % Versions.mockserver
  val hasher = "com.roundeights" %% "hasher" % Versions.hasher
  val newRelic = "com.newrelic.agent.java" % "newrelic-agent" % Versions.newRelic
  def enumeratum(suffix: String) = "com.beachape" %% s"enumeratum$suffix" % Versions.enumeratum

  val baseDepts = Seq(
    typesafeConfig,
    hasher,
    scalaz,
    scalazConcurrent,
    specs2Core % "test" exclude("org.scalaz", "*"),
    specs2Cats % "test",
    specs2Mockito % "test",
    specs2Scalacheck % "test",
    scalacheckShapeless % "test")

  val apiDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    sprayHttp,
    sprayJson,
    sprayRouting,
    sprayTestKit,
    circe("-core"),
    circe("-spray"),
    akkaActor,
    newRelic,
    akkaTestKit % "test",
    cats % "test"))

  val processesDeps = Seq(libraryDependencies ++= baseDepts)

  val servicesDeps = Seq(libraryDependencies ++= baseDepts ++ Seq(
    jodaConvert,
    jodaTime,
    cats,
    doobie("-core"),
    doobie("-contrib-h2"),
    doobie("-contrib-hikari"),
    doobie("-contrib-postgresql"),
    doobie("-contrib-specs2") % "test",
    enumeratum(""),
    enumeratum("-circe"),
    sprayJson,
    flywaydbCore % "test",
    mockserver % "test",
    http4sClient,
    http4sCirce,
    circe("-core"),
    circe("-generic")
  ))
}
