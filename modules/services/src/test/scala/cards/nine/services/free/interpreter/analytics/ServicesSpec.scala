package cards.nine.services.free.interpreter.analytics

import cards.nine.domain.analytics._
import cards.nine.services.free.domain.Ranking.{ Rankings, RankingError, RankingParams, TryRanking }
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

  implicit val configuration: Configuration = Configuration(
    protocol = "http",
    host     = "localhost",
    port     = Option(mockServerPort),
    uri      = paths.batchGet,
    viewId   = TestData.viewId
  )

  val services = Services.services

  "getRanking" should {

    "translate fine to json the answers" in {
      val req = Converters.buildRequest(CountryScope(Country.Spain), viewId, dateRange)
      Encoders.requestBody.apply(req).toString shouldEqual requestBody
    }

    "respond 200 OK and return the Rankings object if a valid access token is provided" in {
      val params = RankingParams(dateRange, 5, AnalyticsToken(auth.valid_token))
      val response = services.getRanking(CountryScope(Country.Spain), params)
      response.unsafePerformSyncAttempt should be_\/-[TryRanking].which {
        content ⇒ content should beXorRight[Rankings]
      }
    }

    /* Return a 401 error message if the auth token is wrong*/
    "respond 401 Unauthorized if the authToken is not authenticated" in {
      val params = RankingParams(dateRange, 5, AnalyticsToken(auth.invalid_token))
      val response = services.getRanking(CountryScope(Country.Spain), params)
      response.unsafePerformSyncAttempt should be_\/-[TryRanking].which {
        content ⇒ content should beXorLeft[RankingError]
      }
    }.pendingUntilFixed("Server gives Unexpected Status")

  }

}

object TestData {

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
        |          "name" : "ga:country"
        |        },
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
        |              "dimensionName" : "ga:country",
        |              "not" : false,
        |              "operator" : "EXACT",
        |              "expressions" : [
        |                "Spain"
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
        |      "pageSize" : 10000,
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
      |        "dimensions": [ "ga:country", "ga:eventCategory", "ga:eventLabel" ],
      |        "metricHeader": { "metricHeaderEntries": [ { "name": "times_used", "type": "INTEGER" } ] }
      |      },
      |      "data": {
      |        "rows": [
      |          {
      |            "dimensions": [ "Spain", "NIGHT", "bloody.mary" ],
      |            "metrics": [ { "values": [ "157" ] } ]
      |          },
      |          {
      |            "dimensions": [ "Spain", "NIGHT", "" ],
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
