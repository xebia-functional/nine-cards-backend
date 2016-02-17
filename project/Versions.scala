import sbt.Keys._
import sbt._

object Versions {
  this: Build =>

  // Build version
  val buildVersion = "1.0.0-SNAPSHOT"

  // Core Libs
  val akka = "2.3.12"
  val scala = "2.11.7"
  val spray = "1.3.3"
  val cats = "0.4.0"

  val googleplayCrawler = "0.3"

  resolvers += Resolver.sonatypeRepo("snapshots")
  val circe = "0.3.0"

  // Test Libs
  val specs2 = "3.7"
  val scalaCheckShapeless = "0.3.2-SNAPSHOT"
}
