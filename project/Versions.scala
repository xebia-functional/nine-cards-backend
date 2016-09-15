import sbt.Keys._
import sbt._

object Versions {

  // Build version
  val buildVersion = "1.0.0-SNAPSHOT"

  // Core Libs
  val akka = "2.3.12"
  val scala = "2.11.8"
  val spray = "1.3.3"
  val cats = "0.4.0"

  resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
  val http4sClient = "0.14.1"

  resolvers += Resolver.sonatypeRepo("snapshots")
  val circe = "0.4.1"
  val jodaTime = "2.9.4"

  val tagSoup = "1.2.1"

  val redisClient = "3.0"
  val embeddedRedis = "0.6"
  val newRelic = "3.29.0"
  val enumeratum = "1.4.13"
  val mockserver = "3.10.4"
  
  // Test Libs
  val specs2 = "3.6.6" // this is the last version depending on scalaz 7.1.4. Http4s needs Scalaz 7.1
  val scalaCheckShapeless = "0.3.2-SNAPSHOT"

}
