package com.fortysevendeg.ninecards.api

import org.specs2.matcher.MatchResult
import spray.httpx.marshalling.Marshaller
import spray.httpx.marshalling.ToResponseMarshaller
import spray.testkit.Specs2RouteTest
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes._
import org.specs2.mutable.Specification
import io.circe._
import io.circe.jawn._

class NineCardsGooglePlayApiSpec
    extends Specification
    with Specs2RouteTest {

  "Nine Cards Google Play Api" should {

    "successfully give the package name for a known Google Play Store app" in {

      val route = new NineCardsGooglePlayApi {
        override def actorRefFactory = system
      }.googlePlayApiRoute

      val headers = List(
        RawHeader("X-Android-ID", "3D4D7FE45C813D3E"),
        RawHeader("X-Google-Play-Token", "DQAAABQBAACJr1nBqQRTmbhS7yFG8NbWqkSXJchcJ5t8FEH-FNNtpk0cU-Xy8-nc_z4fuQV3Sw-INSFK_NuQnafoqNI06nHPD4yaqXVnQbonrVsokBKQnmkQ9SsD0jVZi8bUsC4ehd-w2tmEe7SZ_8vXhw_3f1iNnsrAqkpEvbPkFIo9oZeAq26us2dTo22Ttn3idGoua8Is_PO9EKzItDQD-0T9QXIDDl5otNMG5T4MS9vrbPOEhjorHqGfQJjT8Y10SK2QdgwwyIF2nCGZ6N-E-hbLjD0caXkY7ATpzhOUIJNnBitIs-h52E8JzgHysbYBK9cy6k6Im0WPyHvzXvrwsUK2RTwh-YBpFVSpBACmc89OZKnYE-VfgKHg9SSv1aNrBeEETQE"),
        RawHeader("X-Android-Market-Localization", "es-ES")
      )

      Get("/googleplay/package/com.google.android.googlequicksearchbox") ~> addHeaders(headers) ~> route ~> check {
        status must_== OK
        validateJson(responseAs[String])
      }
    }
  }

  private [this] def validateJson(s: String): MatchResult[Any] = {
    // parse and then traverse the json response to find the list of Strings,
    // and show where the parsing or traversal failed, if so
    val traversal = parse(s).flatMap { doc=>
      doc.hcursor
        .downField("docV2")
        .downField("details")
        .downField("appDetails")
        .get[List[String]]("appCategory")
        .leftMap(e => DecodingFailure(s"Unable to traverse the document produced: [$s]", e.history))
    }.fold(e => Some((e, s)), _ => None) // we only care about the failed state for this test

    traversal must_== None
  }
}
