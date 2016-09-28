import sbt.Keys._
import sbt._

object Versions {

  // Build version
  val buildVersion = "1.0.0-SNAPSHOT"

  // Core Libs
  val akka = "2.4.8"
  val cats = "0.7.2"
  val circe = "0.5.2"
  val enumeratum = "1.4.9"
  val http4s = "0.14.1a"
  val jodaConvert = "1.8.1"
  val jodaTime = "2.9.4"
  val kindProjector = "0.9.0"
  val newRelic = "3.29.0"
  val redisClient = "3.0"
  val scala = "2.11.8"
  val scalaz = "7.2.4"
  val spray = "1.3.3"
  val tagSoup = "1.2.1"
  val typesafeConfig = "1.3.0"

  // Test Libs
  val scalacheckShapeless = "1.1.0-RC3"
  val specs2 = "3.8.4"
  //val specs2 = "3.6.6" // this is the last version depending on scalaz 7.1.4. Http4s needs Scalaz 7.1
  val embeddedRedis = "0.6"
  val mockserver = "3.10.4"

}
