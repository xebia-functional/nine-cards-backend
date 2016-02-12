package com.fortysevendeg.ninecards.api

import org.specs2.matcher.MatchResult
import spray.httpx.marshalling.Marshaller
import spray.httpx.marshalling.ToResponseMarshaller
import spray.testkit.Specs2RouteTest
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes._
import org.specs2.mutable.Specification
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import Domain._

class NineCardsGooglePlayApiSpec
    extends Specification
    with Specs2RouteTest {

  val requestHeaders = List(
    RawHeader("X-Android-ID", "3D4D7FE45C813D3E"),
    RawHeader("X-Google-Play-Token", "DQAAABQBAACJr1nBqQRTmbhS7yFG8NbWqkSXJchcJ5t8FEH-FNNtpk0cU-Xy8-nc_z4fuQV3Sw-INSFK_NuQnafoqNI06nHPD4yaqXVnQbonrVsokBKQnmkQ9SsD0jVZi8bUsC4ehd-w2tmEe7SZ_8vXhw_3f1iNnsrAqkpEvbPkFIo9oZeAq26us2dTo22Ttn3idGoua8Is_PO9EKzItDQD-0T9QXIDDl5otNMG5T4MS9vrbPOEhjorHqGfQJjT8Y10SK2QdgwwyIF2nCGZ6N-E-hbLjD0caXkY7ATpzhOUIJNnBitIs-h52E8JzgHysbYBK9cy6k6Im0WPyHvzXvrwsUK2RTwh-YBpFVSpBACmc89OZKnYE-VfgKHg9SSv1aNrBeEETQE"),
    RawHeader("X-Android-Market-Localization", "es-ES")
  )

  val knownPackages = List(
    "com.google.android.googlequicksearchbox",
    "uk.co.bbc.android.sportdomestic"
  )

  val unknownPackage = "com.package.does.not.exist"

  val allPackages = unknownPackage :: knownPackages

  "Nine Cards Google Play Api" should {

    //TODO persist the response from Google Play, set up something to serve that
    "give the package name for a known single Google Play Store app" in {

      val route = new NineCardsGooglePlayApi {
        override def actorRefFactory = system
      }.googlePlayApiRoute

      Get("/googleplay/package/com.google.android.googlequicksearchbox") ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_== OK
        val response = responseAs[String]
        decode[Item](response).fold(e => Some((e, response)), _ => None) must_== None // we only care about the failure
      }
    }

    "fail with an Internal Server Error when the package is not known" in {

      val route = new NineCardsGooglePlayApi {
        override def actorRefFactory = system
      }.googlePlayApiRoute

      Get(s"/googleplay/package/$unknownPackage") ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_== InternalServerError
      }
    }

    "give the package details for the known packages and highlight the errors" in {

      val route = new NineCardsGooglePlayApi {
        override def actorRefFactory = system
      }.googlePlayApiRoute


      Post("/googleplay/packages/detailed", PackageListRequest(allPackages).asJson.noSpaces) ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_== OK
        val response = responseAs[String]
        decode[PackageDetails](response).fold(e => Some((e, response)), _ => None) must_== None
      }
    }
  }
}
