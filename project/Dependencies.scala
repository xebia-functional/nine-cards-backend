import sbt.Keys._
import sbt._

object Dependencies {

  private def spray(name: String) = "io.spray" %% s"spray-${name}" % Versions.spray

  private val sprayHttp = spray("can")
  private val sprayRouting = spray("routing-shapeless2")
  private val sprayTestKit = spray("testkit") % "test" exclude("org.specs2", "specs2_2.11")

  private def specs(name: String) = "org.specs2" %% s"specs2-${name}" % Versions.specs2

  private val specs2Core = specs("core") % "test"
  private val specs2MatcherExtra = specs("matcher-extra") % "test"
  private val specs2ScalaCheck = specs("scalacheck") % "test"

  private val scalaCheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.12" % Versions.scalaCheckShapeless % "test"

  private def circe(name: String) = "io.circe" %% s"circe-${name}" % Versions.circe

  private val akkaActor = "com.typesafe.akka" %% "akka-actor" % Versions.akka

  private val cats = "org.typelevel" %% "cats" % Versions.cats

  private val embeddedRedis = "com.orange.redis-embedded" % "embedded-redis" % Versions.embeddedRedis % "test"

  private val http4sClient = "org.http4s" %% "http4s-blaze-client" % Versions.http4sClient

  private val redisClient = "net.debasishg" %% "redisclient" % Versions.redisClient

  private val tagSoup = "org.ccil.cowan.tagsoup" % "tagsoup" % Versions.tagSoup

  private val baseDepts = Seq(specs2Core)

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
    redisClient,
    embeddedRedis,
    scalaCheckShapeless,
    specs2ScalaCheck,
    specs2MatcherExtra,
    tagSoup))
}
