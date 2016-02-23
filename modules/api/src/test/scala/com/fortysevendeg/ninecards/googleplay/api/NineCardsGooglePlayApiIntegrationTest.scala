package com.fortysevendeg.ninecards.googleplay.api

import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.Http4sGooglePlayService
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.data.Xor

class NineCardsGooglePlayApiIntegrationTest extends Specification with Specs2RouteTest {

  /*
   * Reasons this test suite may fail:
   *   * The X-Android-ID and X-Google-Play-Token parameters are no longer accepted by Google Play.
   *   * The packages in the field validPackages are no longer in the Google Play Store (Spain, set by the X-Android-Market-Localization header)
   *       - The URL for checking the validity is https://play.google.com/store/apps/details?id=$PACKAGE&hl=es_ES
   */

  val requestHeaders = List(
    RawHeader("X-Android-ID", "3D4D7FE45C813D3E"),
    RawHeader("X-Google-Play-Token", "DQAAABQBAACJr1nBqQRTmbhS7yFG8NbWqkSXJchcJ5t8FEH-FNNtpk0cU-Xy8-nc_z4fuQV3Sw-INSFK_NuQnafoqNI06nHPD4yaqXVnQbonrVsokBKQnmkQ9SsD0jVZi8bUsC4ehd-w2tmEe7SZ_8vXhw_3f1iNnsrAqkpEvbPkFIo9oZeAq26us2dTo22Ttn3idGoua8Is_PO9EKzItDQD-0T9QXIDDl5otNMG5T4MS9vrbPOEhjorHqGfQJjT8Y10SK2QdgwwyIF2nCGZ6N-E-hbLjD0caXkY7ATpzhOUIJNnBitIs-h52E8JzgHysbYBK9cy6k6Im0WPyHvzXvrwsUK2RTwh-YBpFVSpBACmc89OZKnYE-VfgKHg9SSv1aNrBeEETQE"),
    RawHeader("X-Android-Market-Localization", "es-ES")
  )

  val route = new NineCardsGooglePlayApi {
    override def actorRefFactory = system
  }.googlePlayApiRoute(Http4sGooglePlayService.packageRequest _)


  val validPackages = List("air.fisherprice.com.shapesAndColors", "com.rockstargames.gtalcs", "com.ted.android")
  val invalidPackages = List("com.package.does.not.exist", "com.another.invalid.package")
  val allPackages = validPackages.toList ++ invalidPackages

  "Calling a propertly wired NineCardsGoogleApi class" should {
    "Successfully connect to Google Play and give a response for a single package" in {
      Get(s"/googleplay/package/${validPackages.head}") ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== OK

        val docid = decode[Item](responseAs[String]).map(_.docV2.docid)

        docid must_=== Xor.right(validPackages.head)
      }
    }

    "Successfully connect to Google Play and give a response for a list of packages" in {
      Post(s"/googleplay/packages/detailed", PackageListRequest(allPackages).asJson.noSpaces) ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== OK

        val response = decode[PackageDetails](responseAs[String]).map {
          case PackageDetails(errors, items) => (errors.toSet, items.map(_.docV2.docid).toSet)
        }

        response must_=== Xor.right((invalidPackages.toSet, validPackages.toSet))
      }
    }
  }
}
