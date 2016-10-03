package cards.nine.services.free.interpreter.googleplay

import cats.data.Xor
import cards.nine.services.free.domain.GooglePlay._
import cards.nine.services.utils.MockServerService
import org.mockserver.model.HttpRequest._
import org.mockserver.model.HttpResponse._
import org.mockserver.model.{ Header, HttpStatusCode }
import org.specs2.matcher.{ DisjunctionMatchers, Matcher, Matchers, XorMatchers }
import org.specs2.mutable.Specification

trait MockGooglePlayServer extends MockServerService {

  import TestData._

  override val mockServerPort = 9998

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
      .withHeader(headers.contentType)
      .withHeader(headers.contentLength)
      .withBody(resolveManyRequest)
  ).respond(
      response
        .withStatusCode(HttpStatusCode.OK_200.code)
        .withHeader(jsonHeader)
        .withBody(resolveManyValidResponse)
    )

  mockServer.when(
    request
      .withMethod("POST")
      .withPath(paths.recommendations)
      .withHeader(headers.token)
      .withHeader(headers.androidId)
      .withHeader(headers.locale)
      .withHeader(headers.contentType)
      .withHeader(headers.recForAppsContentLength)
      .withBody(recommendationsForAppsRequest)
  ).respond(
      response
        .withStatusCode(HttpStatusCode.OK_200.code)
        .withHeader(jsonHeader)
        .withBody(recommendationsForApps)
    )

  mockServer.when(
    request
      .withMethod("POST")
      .withPath(paths.recommendations)
      .withHeader(headers.token)
      .withHeader(headers.androidId)
      .withHeader(headers.locale)
      .withHeader(headers.contentType)
      .withHeader(headers.emptyRecForAppsContentLength)
      .withBody(recommendationsForAppsEmptyRequest)
  ).respond(
      response
        .withStatusCode(HttpStatusCode.OK_200.code)
        .withHeader(jsonHeader)
        .withBody(recommendationsForAppsEmptyResponse)
    )

  mockServer.when(
    request
      .withMethod("POST")
      .withPath(paths.recommendationsByCountries)
      .withHeader(headers.token)
      .withHeader(headers.androidId)
      .withHeader(headers.locale)
      .withHeader(headers.contentType)
      .withHeader(headers.recByCategoryContentLength)
      .withBody(recommendationsByCategoryRequest)
  ).respond(
      response
        .withStatusCode(HttpStatusCode.OK_200.code)
        .withHeader(jsonHeader)
        .withBody(recommendationsForAll)
    )

  mockServer.when(
    request
      .withMethod("POST")
      .withPath(paths.freeRecommendationsByCountries)
      .withHeader(headers.token)
      .withHeader(headers.androidId)
      .withHeader(headers.locale)
      .withHeader(headers.contentType)
      .withHeader(headers.recByCategoryContentLength)
      .withBody(recommendationsByCategoryRequest)
  ).respond(
      response
        .withStatusCode(HttpStatusCode.OK_200.code)
        .withHeader(jsonHeader)
        .withBody(recommendationsForFree)
    )

  mockServer.when(
    request
      .withMethod("POST")
      .withPath(paths.paidRecommendationsByCountries)
      .withHeader(headers.token)
      .withHeader(headers.androidId)
      .withHeader(headers.locale)
      .withHeader(headers.contentType)
      .withHeader(headers.recByCategoryContentLength)
      .withBody(recommendationsByCategoryRequest)
  ).respond(
      response
        .withStatusCode(HttpStatusCode.OK_200.code)
        .withHeader(jsonHeader)
        .withBody(recommendationsForPaid)
    )
}

class GooglePlayServicesSpec
  extends Specification
  with Matchers
  with DisjunctionMatchers
  with MockGooglePlayServer
  with XorMatchers {

  import TestData._

  val tokenIdParameterName = "id_token"

  implicit val googlePlayConfiguration = Configuration(
    protocol            = "http",
    host                = "localhost",
    port                = Option(mockServerPort),
    recommendationsPath = paths.recommendations,
    resolveOnePath      = paths.resolveOne,
    resolveManyPath     = paths.resolveMany
  )

  val services = Services.services

  def recommendationIsFreeMatcher(isFree: Boolean): Matcher[Recommendation] = { rec: Recommendation ⇒
    rec.free must_== isFree
  }

  "resolveOne" should {

    "return the App object when a valid package name is provided" in {
      val response = services.resolveOne(appsNames.italy, auth.params)
      response.unsafePerformSyncAttempt should be_\/-[String Xor AppInfo].which {
        content ⇒ content should beXorRight[AppInfo]
      }
    }

  }

  "resolveMany" should {
    "return the list of apps that are valid" in {
      val response = services.resolveMany(List(appsNames.italy, appsNames.prussia), auth.params)
      response.unsafePerformSyncAttempt should be_\/-[AppsInfo]
    }
  }

  "recommendByCategory" should {
    "return a list of recommended apps for the given category" in {
      val response = services.recommendByCategory(
        category         = "COUNTRY",
        filter           = "ALL",
        excludesPackages = List(appsNames.france),
        limit            = limit,
        auth             = auth.params
      )
      response.unsafePerformSyncAttempt should be_\/-[Recommendations].which { rec ⇒
        rec.apps must haveSize(2)
      }
    }
    "return a list of free recommended apps for the given category" in {
      val response = services.recommendByCategory(
        category         = "COUNTRY",
        filter           = "FREE",
        excludesPackages = List(appsNames.france),
        limit            = limit,
        auth             = auth.params
      )
      response.unsafePerformSyncAttempt should be_\/-[Recommendations].which { rec ⇒
        rec.apps must haveSize(1)
        rec.apps must contain(recommendationIsFreeMatcher(true)).forall
      }
    }
    "return a list of paid recommended apps for the given category" in {
      val response = services.recommendByCategory(
        category         = "COUNTRY",
        filter           = "PAID",
        excludesPackages = List(appsNames.france),
        limit            = limit,
        auth             = auth.params
      )

      response.unsafePerformSyncAttempt should be_\/-[Recommendations].which { rec ⇒
        rec.apps must haveSize(1)
        rec.apps must contain(recommendationIsFreeMatcher(false)).forall
      }
    }
  }

  "recommendationsForApps" should {
    "return an empty list of recommended apps if an empty list of packages is given" in {
      val response = services.recommendationsForApps(
        packageNames     = Nil,
        excludesPackages = Nil,
        limitByApp       = numPerApp,
        limit            = limit,
        auth             = auth.params
      )
      response.unsafePerformSyncAttempt should be_\/-[Recommendations].which { rec ⇒
        rec.apps must beEmpty
      }
    }
    "return a list of recommended apps for the given list of packages" in {
      val response = services.recommendationsForApps(
        packageNames     = List(appsNames.italy, appsNames.prussia),
        excludesPackages = List(appsNames.france),
        limitByApp       = numPerApp,
        limit            = limit,
        auth             = auth.params
      )
      response.unsafePerformSyncAttempt should be_\/-[Recommendations].which { rec ⇒
        rec.apps must haveSize(2)
      }
    }
  }
}

object TestData {

  object appsNames {
    val italy = "earth.europe.italy"
    val prussia = "earth.europe.prussia"
    val france = "earth.europe.france"
  }

  val limit = 20

  val numPerApp = 25

  val recommendationsForApps =
    s"""
       |{
       |  "apps": [
       |    {
       |      "packageName" : "${appsNames.italy}",
       |      "title" : "Italy",
       |      "free" : true,
       |      "icon" : "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
       |      "stars" : 3.66,
       |      "downloads" : "542412",
       |      "screenshots" : [
       |        "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
       |        "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym7s-mr_M=w300-rw"
       |      ]
       |    },
       |    {
       |      "packageName" : "${appsNames.prussia}",
       |      "title" : "Prussia",
       |      "free" : false,
       |      "icon" : "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
       |      "stars" : 3.66,
       |      "downloads" : "542412",
       |      "screenshots" : [
       |        "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
       |        "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym7s-mr_M=w300-rw"
       |      ]
       |    }
       |  ]
       |}
    """.stripMargin

  val recommendationsForAppsEmptyResponse =
    s"""
       |{
       |  "apps": []
       |}
    """.stripMargin

  val recommendationsForAll =
    s"""
       |{
       |  "apps": [
       |    {
       |      "packageName" : "${appsNames.italy}",
       |      "title" : "Italy",
       |      "free" : true,
       |      "icon" : "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
       |      "stars" : 3.66,
       |      "downloads" : "542412",
       |      "screenshots" : [
       |        "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
       |        "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym7s-mr_M=w300-rw"
       |      ]
       |    },
       |    {
       |      "packageName" : "${appsNames.prussia}",
       |      "title" : "Prussia",
       |      "free" : false,
       |      "icon" : "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
       |      "stars" : 3.66,
       |      "downloads" : "542412",
       |      "screenshots" : [
       |        "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
       |        "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym7s-mr_M=w300-rw"
       |      ]
       |    }
       |  ]
       |}
    """.stripMargin

  val recommendationsForFree =
    s"""
      |{
      |  "apps": [
      |    {
      |      "packageName" : "${appsNames.italy}",
      |      "title" : "Italy",
      |      "free" : true,
      |      "icon" : "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
      |      "stars" : 3.66,
      |      "downloads" : "542412",
      |      "screenshots" : [
      |        "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
      |        "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym7s-mr_M=w300-rw"
      |      ]
      |    }
      |  ]
      |}
    """.stripMargin

  val recommendationsForPaid =
    s"""
      |{
      |  "apps": [
      |    {
      |      "packageName" : "${appsNames.prussia}",
      |      "title" : "Prussia",
      |      "free" : false,
      |      "icon" : "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
      |      "stars" : 3.66,
      |      "downloads" : "542412",
      |      "screenshots" : [
      |        "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
      |        "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym7s-mr_M=w300-rw"
      |      ]
      |    }
      |  ]
      |}
    """.stripMargin

  val resolvedPackage = s"""
      |{
      |  "packageName" : "${appsNames.italy}",
      |  "title" : "Italy",
      |  "free" : true,
      |  "icon" : "https://lh5.ggpht.com/D5mlVsxok0icv00iCkwirgrncmym6s-mr_M=w300-rw",
      |  "stars" : 3.66,
      |  "downloads" : "542412",
      |  "categories" : [ "Country" ]
      |}
    """.stripMargin

  val resolveManyRequest = s"""{"items":["${appsNames.italy}","${appsNames.prussia}"]}"""

  val recommendationsByCategoryRequest =
    s"""{"excludedApps":["${appsNames.france}"],"maxTotal":$limit}""".stripMargin

  val recommendationsForAppsEmptyRequest =
    s"""{"searchByApps":[],"numPerApp":$numPerApp,"excludedApps":[],"maxTotal":$limit}""".stripMargin

  val recommendationsForAppsRequest =
    s"""{"searchByApps":["${appsNames.italy}","${appsNames.prussia}"],"numPerApp":$numPerApp,"excludedApps":["${appsNames.france}"],"maxTotal":$limit}""".stripMargin

  val resolveManyValidResponse = s"""
      |{
      |  "missing" : [ "${appsNames.prussia}" ],
      |  "apps" : [ $resolvedPackage ]
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
    val locale = new Header("X-Android-Market-Localization", auth.localization)
    val contentType = new Header("Content-Type", "application/json")
    val contentLength = new Header("Content-Length", resolveManyRequest.length.toString)
    val recByCategoryContentLength = new Header("Content-Length", recommendationsByCategoryRequest.length.toString)
    val emptyRecForAppsContentLength = new Header("Content-Length", recommendationsForAppsEmptyRequest.length.toString)
    val recForAppsContentLength = new Header("Content-Length", recommendationsForAppsRequest.length.toString)
  }

  object paths {
    val recommendations = "/googleplay/recommendations"
    val recommendationsByCountries = "/googleplay/recommendations/COUNTRY/ALL"
    val freeRecommendationsByCountries = "/googleplay/recommendations/COUNTRY/FREE"
    val paidRecommendationsByCountries = "/googleplay/recommendations/COUNTRY/PAID"
    val resolveOne = "/googleplay/cards"
    val resolveMany = "/googleplay/cards"
  }

}
