package cards.nine.services.free.interpreter.analytics

import cards.nine.commons.NineCardsErrors.NineCardsError
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.config.Domain.GoogleAnalyticsConfiguration
import cards.nine.domain.analytics._
import cards.nine.services.free.domain.Ranking.GoogleAnalyticsRanking
import cards.nine.services.utils.MockServerService
import org.joda.time.{ DateTime, DateTimeZone }
import org.mockserver.model.HttpRequest.{ request ⇒ mockRequest }
import org.mockserver.model.HttpResponse.{ response ⇒ mockResponse }
import org.mockserver.model.{ Header, HttpStatusCode, JsonBody }
import org.specs2.matcher.{ DisjunctionMatchers, Matchers, XorMatchers }
import org.specs2.mutable.Specification

trait MockServer extends MockServerService {

  import TestData._

  override val mockServerPort = 9997

  /* First case: successful request*/
  mockServer.when(
    mockRequest
      .withMethod("POST")
      .withPath(paths.batchGet)
      .withHeader(headers.contentType)
      .withBody(new JsonBody(requestBody))
      .withHeader(headers.authorization(auth.valid_token))
  ).respond(
      mockResponse
        .withStatusCode(HttpStatusCode.OK_200.code)
        .withHeader(jsonHeader)
        .withBody(new JsonBody(rankingsResponse))
    )

  mockServer.when(
    mockRequest
      .withMethod("POST")
      .withPath(paths.batchGet)
      .withHeader(headers.contentType)
      .withBody(new JsonBody(requestBody))
      .withHeader(headers.authorization(auth.invalid_token))
  ).respond(
      mockResponse
        .withStatusCode(HttpStatusCode.UNAUTHORIZED_401.code)
        .withHeader(jsonHeader)
        .withBody(new JsonBody(unauthenticated_error))
    )

}

class ServicesSpec
  extends Specification
  with MockServer
  with Matchers
  with DisjunctionMatchers
  with XorMatchers {

  import TestData._

  val configuration: GoogleAnalyticsConfiguration = GoogleAnalyticsConfiguration(
    protocol = "http",
    host     = "localhost",
    port     = Option(mockServerPort),
    path     = paths.batchGet,
    viewId   = TestData.viewId
  )

  val services = Services.services(configuration)

  "getRanking" should {

    "respond 200 OK and return the Rankings object if a valid access token is provided" in {
      val params = RankingParams(dateRange, 5, AnalyticsToken(auth.valid_token))
      val response = services.getRanking(countryCode, categories, params)
      response.unsafePerformSyncAttempt should be_\/-[Result[GoogleAnalyticsRanking]].which {
        content ⇒ content should beRight[GoogleAnalyticsRanking]
      }
    }

    /* Return a 401 error message if the auth token is wrong*/
    "respond 401 Unauthorized if the authToken is not authenticated" in {
      val params = RankingParams(dateRange, 5, AnalyticsToken(auth.invalid_token))
      val response = services.getRanking(countryCode, categories, params)
      response.unsafePerformSyncAttempt should be_\/-[Result[GoogleAnalyticsRanking]].which {
        content ⇒ content should beLeft[NineCardsError]
      }
    }.pendingUntilFixed("Server gives Unexpected Status")

  }

}

object TestData {

  val categories = List("SOCIAL")

  val countryCode = Option(CountryIsoCode("ES"))

  val viewId = "analytics_view_id"

  val dateRange: DateRange = DateRange(
    startDate = new DateTime(2010, 1, 1, 0, 0, DateTimeZone.UTC),
    endDate   = new DateTime(2010, 1, 31, 0, 0, DateTimeZone.UTC)
  )

  val requestBody: String =
    s"""|{
        |  "reportRequests" : [
        |    {
        |      "viewId" : "analytics_view_id",
        |      "dimensions" : [
        |        {
        |          "name" : "ga:eventCategory"
        |        },
        |        {
        |          "name" : "ga:eventLabel"
        |        }
        |      ],
        |      "metrics" : [
        |        {
        |          "expression" : "ga:eventValue",
        |          "alias" : "times_used",
        |          "formattingType" : "INTEGER"
        |        }
        |      ],
        |      "dateRanges" : [
        |        {
        |          "startDate" : "2010-01-01",
        |          "endDate" : "2010-01-31"
        |        }
        |      ],
        |      "dimensionFilterClauses" : [
        |        {
        |          "operator" : "OPERATOR_UNSPECIFIED",
        |          "filters" : [
        |            {
        |              "dimensionName" : "ga:eventCategory",
        |              "not" : false,
        |              "operator" : "EXACT",
        |              "expressions" : [
        |                "SOCIAL"
        |              ],
        |              "caseSensitive" : false
        |            }
        |          ]
        |        },
        |        {
        |          "operator" : "OPERATOR_UNSPECIFIED",
        |          "filters" : [
        |            {
        |              "dimensionName" : "ga:countryIsoCode",
        |              "not" : false,
        |              "operator" : "EXACT",
        |              "expressions" : [
        |                "ES"
        |              ],
        |              "caseSensitive" : false
        |            }
        |          ]
        |        }
        |      ],
        |      "orderBys" : [
        |        {
        |          "fieldName" : "ga:eventCategory",
        |          "orderType" : "VALUE",
        |          "sortOrder" : "ASCENDING"
        |        },
        |        {
        |          "fieldName" : "ga:eventValue",
        |          "orderType" : "VALUE",
        |          "sortOrder" : "DESCENDING"
        |        }
        |      ],
        |      "includeEmptyRows" : true,
        |      "pageSize" : 5,
        |      "hideTotals" : true,
        |      "hideValueRanges" : true
        |    }
        |  ]
        |}""".stripMargin

  val rankingsResponse =
    s"""
      |{
      |  "reports" : [
      |    {
      |      "columnHeader": {
      |        "dimensions": [ "ga:eventCategory", "ga:eventLabel" ],
      |        "metricHeader": { "metricHeaderEntries": [ { "name": "times_used", "type": "INTEGER" } ] }
      |      },
      |      "data": {
      |        "rows": [
      |          {
      |            "dimensions": [ "NIGHT", "bloody.mary" ],
      |            "metrics": [ { "values": [ "157" ] } ]
      |          },
      |          {
      |            "dimensions": [ "NIGHT", "" ],
      |            "metrics": [ { "values": [ "141" ] } ]
      |          }
      |        ],
      |        "rowCount": 2,
      |        "isDataGolden": true
      |      }
      |    }
      |  ]
      |}
    """.stripMargin

  val unauthenticated_error = """
      |{ "error": {
      |    "code": 401,
      |    "message": "Request had invalid authentication credentials.",
      |    "status": "UNAUTHENTICATED"
      |}}
    """.stripMargin

  object auth {
    val valid_token = "granted"
    val invalid_token = "denied"
  }

  object headers {
    def authorization(token: String) = new Header("Authorization", s"Bearer $token")
    val contentType = new Header("Content-Type", "application/json")
    def contentLength(body: String) = new Header("Content-Length", body.length.toString)
  }

  object paths {
    val batchGet = "/v4/reports:batchGet"
  }

}
