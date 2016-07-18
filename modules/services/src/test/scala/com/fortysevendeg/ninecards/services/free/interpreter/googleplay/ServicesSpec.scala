package com.fortysevendeg.ninecards.services.free.interpreter.googleplay

import cats.data.Xor
import com.fortysevendeg.ninecards.services.free.domain.GooglePlay._
import com.fortysevendeg.ninecards.services.utils.{ MockServerService, XorMatchers }
import org.mockserver.model.HttpRequest._
import org.mockserver.model.HttpResponse._
import org.mockserver.model.{ Header, HttpStatusCode }
import org.specs2.matcher.{ DisjunctionMatchers, Matchers }
import org.specs2.mutable.Specification

trait MockGooglePlayServer extends MockServerService {

  import TestData._

  mockServer.when(
    request
      .withMethod("GET")
      .withPath(s"${paths.resolveOne}/${appsNames.italy}")
      .withHeader(headers.token)
      .withHeader(headers.androidId)
      .withHeader(headers.locale)
  ).respond(
      response
        .withStatusCode(HttpStatusCode.OK_200.code)
        .withHeader(jsonHeader)
        .withBody(resolvedPackage)
    )

  mockServer.when(
    request
      .withMethod("GET")
      .withPath(s"${paths.resolveOne}/${appsNames.prussia}")
      .withHeader(headers.token)
      .withHeader(headers.androidId)
      .withHeader(headers.locale)
  ).respond(
      response
        .withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500.code)
        .withBody(failure)
    )

  mockServer.when(
    request
      .withMethod("POST")
      .withPath(paths.resolveMany)
      .withHeader(headers.token)
      .withHeader(headers.androidId)
      .withHeader(headers.locale)
      .withBody(resolveManyRequest)
  ).respond(
      response
        .withStatusCode(HttpStatusCode.OK_200.code)
        .withHeader(jsonHeader)
        .withBody(resolveManyValidResponse)
    )

}

class GooglePlayServicesSpec
  extends Specification
  with Matchers
  with DisjunctionMatchers
  with MockGooglePlayServer {

  import TestData._
  import XorMatchers._

  val tokenIdParameterName = "id_token"

  implicit val googlePlayConfiguration = Configuration(
    protocol       = "http",
    host           = "localhost",
    port           = Option(mockServerPort),
    resolveOneUri  = paths.resolveOne,
    resolveManyUri = paths.resolveMany
  )

  val services = Services.services

  "resolveOne" should {

    "return the App object when a valid package name is provided" in {
      val response = services.resolveOne(appsNames.italy, auth.params)
      response.attemptRun should be_\/-[UnresolvedApp Xor AppCard].which {
        content ⇒ content should beXorRight[AppCard]
      }
    }

  }

  "resolveMany" should {
    "return the list of apps that are valid" in {
      val response = services.resolveMany(List(appsNames.italy, appsNames.prussia), auth.params)
      response.attemptRun should be_\/-[AppsCards]
    }
  }

}

object TestData {

  object appsNames {
    val italy = "earth.europe.italy"
    val prussia = "earth.europe.prussia"
  }

  val resolvedPackage = s"""
      |{
      |  "docV2" : {
      |    "title" : "Italy",
      |    "creator" : "Vittorio Emmanuelle",
      |    "docid" : "${appsNames.italy}",
      |    "details" : { "appDetails" : {
      |      "appCategory" : [ "Country" ],
      |      "numDownloads" : "542412",
      |      "permission" : []
      |    }},
      |    "aggregateRating" : {
      |      "ratingsCount" : 15,
      |      "oneStarRatings" : 1,
      |      "twoStarRatings" : 2,
      |      "threeStarRatings" : 3,
      |      "fourStarRatings" : 4,
      |      "fiveStarRatings" : 5,
      |      "starRating" : 3.66
      |    },
      |    "image" : [],
      |    "offer" : []
      |  }
      |}
    """.stripMargin

  val resolveManyRequest = s"""[ "${appsNames.italy}", "${appsNames.prussia}" ]"""

  val resolveManyValidResponse = s"""
      |{
      |  "errors" : [ "${appsNames.prussia}" ],
      |  "items" : [ $resolvedPackage ]
      |}
    """.stripMargin

  val failure = "Cannot find item!"

  object auth {
    val androidId = "12345"
    val localization = "en_GB"
    val token = "m52_9876"
    val params = AuthParams(androidId, Some(localization), token)
  }

  object headers {
    val androidId = new Header("X-Android-ID", auth.androidId)
    val token = new Header("X-Google-Play-Token", auth.token)
    val locale = new Header("X-Android-Maket-Localization", auth.localization)
  }

  object paths {
    val resolveOne = "/googleplay/package"
    val resolveMany = "/googleplay/packages/detailed"
  }

}
