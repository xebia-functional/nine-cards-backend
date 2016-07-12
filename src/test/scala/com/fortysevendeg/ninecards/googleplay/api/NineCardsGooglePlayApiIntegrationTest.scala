package com.fortysevendeg.ninecards.googleplay.api

import cats.data.Xor
import cats.syntax.xor._
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.config.NineCardsConfig.getConfigValue
import com.fortysevendeg.ninecards.googleplay.TestConfig._
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.{ Http4sGooglePlayApiClient, Http4sGooglePlayWebScraper, TaskInterpreter }
import io.circe.generic.auto._
import io.circe.parser._
import org.specs2.mutable.Specification
import scala.concurrent.duration._
import scalaz.concurrent.Task
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes._
import spray.testkit.Specs2RouteTest

class NineCardsGooglePlayApiIntegrationTest extends Specification with Specs2RouteTest {

  /*
   * Reasons this test suite may fail:
   *   * The X-Android-ID and X-Google-Play-Token parameters are no longer accepted by Google Play.
   *   * The packages in the field validPackages are no longer in the Google Play Store (Spain, set by the X-Android-Market-Localization header)
   *       - The URL for checking the validity is https://play.google.com/store/apps/details?id=$PACKAGE&hl=es_ES
   */

  import NineCardsMarshallers._

  implicit val defaultTimeout = RouteTestTimeout(20.seconds)

  val requestHeaders = List(
    RawHeader(Headers.androidId, androidId.value),
    RawHeader(Headers.token, token.value),
    RawHeader(Headers.localization, localization.value)
  )

  implicit val i = {
    val client = org.http4s.client.blaze.PooledHttp1Client()
    val apiClient = new Http4sGooglePlayApiClient( getConfigValue("googleplay.api.endpoint") , client)
    val webClient = new Http4sGooglePlayWebScraper( getConfigValue("googleplay.web.endpoint") , client)
    TaskInterpreter(apiClient, webClient)
  }

  val route = NineCardsGooglePlayApi.googlePlayApiRoute[Task]

  val validPackages = List("air.fisherprice.com.shapesAndColors", "com.rockstargames.gtalcs", "com.ted.android")
  val invalidPackages = List("com.package.does.not.exist", "com.another.invalid.package")
  val allPackages = validPackages ++ invalidPackages

  "Calling a propertly wired NineCardsGoogleApi class" should {
    "Successfully connect to Google Play and give a response for a single package" in {
      Get(s"/googleplay/package/${validPackages.head}") ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== OK

        val docid = decode[Item](responseAs[String]).map(_.docV2.docid)

        docid must_=== validPackages.head.right
      }
    }

    "Successfully connect to Google Play and give a response for a list of packages" in {
      Post(s"/googleplay/packages/detailed", PackageList(allPackages)) ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== OK

        val response = decode[PackageDetails](responseAs[String]).map {
          case PackageDetails(errors, items) => (errors.toSet, items.map(_.docV2.docid).toSet)
        }

        response must_=== (invalidPackages.toSet, validPackages.toSet).right
      }
    }
  }
}
