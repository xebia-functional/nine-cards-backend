package cards.nine.api.applications

import akka.actor.ActorSystem
import akka.testkit._
import cards.nine.api.{ AuthHeadersRejectionHandler, NineCardsExceptionHandler }
import cards.nine.api.NineCardsHeaders._
import cards.nine.api.TestData.{ Headers, androidId, authToken, failingAuthToken, sessionToken, userId }
import cards.nine.api.applications.TestData._
import cards.nine.commons.NineCardsErrors.AuthTokenNotValid
import cards.nine.commons.NineCardsService
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.commons.config.NineCardsConfig
import cards.nine.domain.account._
import cards.nine.processes.NineCardsServices._
import cards.nine.processes._
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.applications.ApplicationProcesses
import cards.nine.processes.rankings.RankingProcesses
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http.{ BasicHttpCredentials, HttpRequest, StatusCodes, Uri }
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

import scala.concurrent.duration.DurationInt

trait ApplicationsApiSpecification
  extends Specification
  with AuthHeadersRejectionHandler
  with HttpService
  with JsonFormats
  with Matchers
  with Mockito
  with NineCardsExceptionHandler
  with Specs2RouteTest {

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(20.second dilated system)

  implicit def actorRefFactory = system

  trait BasicScope extends Scope {

    implicit val applicationProcesses: ApplicationProcesses[NineCardsServices] = mock[ApplicationProcesses[NineCardsServices]]

    implicit val rankingProcesses: RankingProcesses[NineCardsServices] = mock[RankingProcesses[NineCardsServices]]

    implicit val accountProcesses: AccountProcesses[NineCardsServices] = mock[AccountProcesses[NineCardsServices]]

    implicit val config: NineCardsConfiguration = NineCardsConfig.nineCardsConfiguration

    val routes = sealRoute(new ApplicationsApi().route)

    accountProcesses.checkAuthToken(
      sessionToken = SessionToken(mockEq(sessionToken.value)),
      androidId    = AndroidId(mockEq(androidId.value)),
      authToken    = mockEq(authToken),
      requestUri   = any[String]
    ) returns NineCardsService.right(userId)
  }

  trait SuccessfulScope extends BasicScope {

    applicationProcesses.getAppsInfo(any, any) returns
      NineCardsService.right(getAppsInfoResponse)

    applicationProcesses.getRecommendationsByCategory(any, any, any, any, any) returns
      NineCardsService.right(getRecommendationsByCategoryResponse)

    applicationProcesses.getRecommendationsForApps(any, any, any, any, any) returns
      NineCardsService.right(getRecommendationsByCategoryResponse)

    rankingProcesses.getRankedDeviceApps(any, any) returns
      NineCardsService.right(getRankedAppsResponse).value

    rankingProcesses.getRankedAppsByMoment(any, any, any, any) returns
      NineCardsService.right(getRankedAppsResponse).value

    rankingProcesses.getRankedWidgets(any, any, any, any) returns
      NineCardsService.right(getRankedWidgetsResponse).value

  }

  trait UnsuccessfulScope extends BasicScope {

    accountProcesses.checkAuthToken(
      sessionToken = SessionToken(mockEq(sessionToken.value)),
      androidId    = AndroidId(mockEq(androidId.value)),
      authToken    = mockEq(failingAuthToken),
      requestUri   = any[String]
    ) returns NineCardsService.left(AuthTokenNotValid("The provided auth token is not valid"))

  }

  trait FailingScope extends BasicScope {

    accountProcesses.checkAuthToken(
      sessionToken = SessionToken(mockEq(sessionToken.value)),
      androidId    = AndroidId(mockEq(androidId.value)),
      authToken    = mockEq(failingAuthToken),
      requestUri   = any[String]
    ) returns NineCardsService.right(userId)

    rankingProcesses.getRankedDeviceApps(any, any) returns NineCardsService.right(getRankedAppsResponse).value
  }

}

class ApplicationsApiSpec
  extends ApplicationsApiSpecification {

  private[this] def unauthorizedNoHeaders(request: HttpRequest) = {

    "return a 401 Unauthorized status code if no headers are provided" in new BasicScope {
      request ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "return a 401 Unauthorized status code if some of the headers aren't provided" in new BasicScope {
      request ~> addHeader(RawHeader(headerAndroidId, androidId.value)) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "return a 401 Unauthorized status code if a wrong credential is provided" in new UnsuccessfulScope {
      request ~> addHeaders(Headers.failingUserInfoHeaders) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "return a 401 Unauthorized status code if a persistence error happens" in new FailingScope {
      request ~> addHeaders(Headers.failingUserInfoHeaders) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }.pendingUntilFixed("Pending using EitherT")

  }

  private[this] def internalServerError(request: HttpRequest) = {
    "return 500 Internal Server Error status code if a persistence error happens" in new FailingScope {
      request ~> addHeaders(Headers.userInfoHeaders) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.InternalServerError.intValue
      }
    }.pendingUntilFixed("Pending using EitherT")
  }

  private[this] def badRequestEmptyBody(request: HttpRequest) = {
    "return a 400 BadRequest if no body is provided" in new BasicScope {
      request ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.BadRequest.intValue
      }
    }
  }

  private[this] def authenticatedBadRequestEmptyBody(request: HttpRequest) = {
    "return a 400 BadRequest if no body is provided" in new BasicScope {
      request ~> addHeaders(Headers.userInfoHeaders) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.BadRequest.intValue
      }
    }
  }

  private[this] def successOk(request: HttpRequest) = {
    "return a 200 OK Status code if the operation was carried out" in new SuccessfulScope {
      request ~> addHeaders(Headers.userInfoHeaders) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.OK.intValue
      }
    }
  }

  "POST /applications/categorize" should {

    val request = Post(
      uri     = Paths.categorize,
      content = apiGetAppsInfoRequest
    ) ~> addHeaders(Headers.googlePlayHeaders)

    authenticatedBadRequestEmptyBody(Post(Paths.categorize))

    unauthorizedNoHeaders(request)

    successOk(request)
  }

  "POST /applications/details" should {

    val request = Post(
      uri     = Paths.details,
      content = apiGetAppsInfoRequest
    ) ~> addHeaders(Headers.googlePlayHeaders)

    authenticatedBadRequestEmptyBody(Post(Paths.details))

    unauthorizedNoHeaders(request)

    successOk(request)
  }

  """POST /applications/details?slice=icon, the variant to get only title and icon""" should {

    val request = Post(
      uri     = Uri(
        path  = Uri.Path(Paths.details),
        query = Uri.Query("?slice=icon")
      ),
      content = apiGetAppsInfoRequest
    ) ~> addHeaders(Headers.googlePlayHeaders)

    authenticatedBadRequestEmptyBody(Post(Paths.details))

    unauthorizedNoHeaders(request)

    successOk(request)
  }

  """ PUT /applications/details/{packageId}, the endpoint to store a card in the cache""" should {

    val validPackage = "a.valid.package"

    def request(packageId: String) = Put(
      uri     = s"${Paths.details}/$packageId",
      content = setAppInfoRequest
    )

    "Respond NotFound if the package name is badformed" in new BasicScope {
      request("++package++") ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue

      }
    }

    "respond Unauthorized if Basic Auth is missing" in new BasicScope {
      request(validPackage) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "respond Unauthorized if there are auth headers, but unknown" in new BasicScope {
      val invalidCredentials = BasicHttpCredentials("Jon", "Doe")

      request(validPackage) ~> addCredentials(invalidCredentials) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "respond OK if the Basic Http Credentials are in the config " in new SuccessfulScope {
      applicationProcesses.storeCard(any) returns NineCardsService.right(Unit)

      val (user, password) = config.editors.head
      val credentials = BasicHttpCredentials(user, password)
      request(validPackage) ~> addCredentials(credentials) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.OK.intValue
      }
    }

  }

  "POST /applications/rank" should {

    val request = Post(
      uri     = Paths.rankApps,
      content = apiRankAppsRequest
    )

    authenticatedBadRequestEmptyBody(Post(Paths.rankApps))

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "POST /applications/rank-by-moment" should {

    val request = Post(
      uri     = Paths.rankAppsByMoments,
      content = apiRankByMomentsRequest
    )

    authenticatedBadRequestEmptyBody(Post(Paths.rankApps))

    unauthorizedNoHeaders(request)

    successOk(request)
  }

}
