package cards.nine.api

import akka.actor.ActorSystem
import akka.testkit._
import cards.nine.api.NineCardsHeaders._
import cards.nine.api.TestData._
import cards.nine.commons.NineCardsErrors.{ AuthTokenNotValid, WrongEmailAccount }
import cards.nine.commons.NineCardsService
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.commons.config.NineCardsConfig
import cards.nine.domain.account._
import cards.nine.processes.NineCardsServices._
import cards.nine.processes._
import cards.nine.processes.applications.ApplicationProcesses
import cards.nine.processes.messages.UserMessages._
import cats.free.Free
import cats.syntax.either._
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http.{ HttpRequest, MediaTypes, StatusCodes }
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

import scala.concurrent.duration.DurationInt

trait NineCardsApiSpecification
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

    implicit val userProcesses: UserProcesses[NineCardsServices] = mock[UserProcesses[NineCardsServices]]

    implicit val googleApiProcesses: GoogleApiProcesses[NineCardsServices] = mock[GoogleApiProcesses[NineCardsServices]]

    implicit val applicationProcesses: ApplicationProcesses[NineCardsServices] = mock[ApplicationProcesses[NineCardsServices]]

    implicit val rankingProcesses: RankingProcesses[NineCardsServices] = mock[RankingProcesses[NineCardsServices]]

    implicit val config: NineCardsConfiguration = NineCardsConfig.nineCardsConfiguration

    val nineCardsApi = new NineCardsRoutes().nineCardsRoutes

    userProcesses.checkAuthToken(
      sessionToken = SessionToken(mockEq(sessionToken.value)),
      androidId    = AndroidId(mockEq(androidId.value)),
      authToken    = mockEq(authToken),
      requestUri   = any[String]
    ) returns NineCardsService.right(userId)
  }

  trait SuccessfulScope extends BasicScope {

    googleApiProcesses.checkGoogleTokenId(email, tokenId) returns NineCardsService.right(Unit)

    userProcesses.signUpUser(any[LoginRequest]) returns NineCardsService.right(Messages.loginResponse)

    userProcesses.updateInstallation(mockEq(Messages.updateInstallationRequest)) returns
      NineCardsService.right(Messages.updateInstallationResponse)

    rankingProcesses.getRanking(any) returns Free.pure(Either.right(Messages.rankings.getResponse))

    rankingProcesses.reloadRankingByScope(any, any) returns
      Free.pure(Either.right(Messages.rankings.reloadResponse))

    applicationProcesses.getRecommendationsByCategory(any, any, any, any, any) returns
      NineCardsService.right(Messages.getRecommendationsByCategoryResponse)

    applicationProcesses.getRecommendationsForApps(any, any, any, any, any) returns
      NineCardsService.right(Messages.getRecommendationsByCategoryResponse)

    rankingProcesses.getRankedWidgets(any, any, any, any) returns
      NineCardsService.right(Messages.getRankedWidgetsResponse).value

  }

  trait UnsuccessfulScope extends BasicScope {

    googleApiProcesses.checkGoogleTokenId(email, tokenId) returns
      NineCardsService.left(WrongEmailAccount("The given email account is not valid"))

    userProcesses.checkAuthToken(
      sessionToken = SessionToken(mockEq(sessionToken.value)),
      androidId    = AndroidId(mockEq(androidId.value)),
      authToken    = mockEq(failingAuthToken),
      requestUri   = any[String]
    ) returns NineCardsService.left(AuthTokenNotValid("The provided auth token is not valid"))

  }

  trait FailingScope extends BasicScope {

    googleApiProcesses.checkGoogleTokenId(email, tokenId) returns NineCardsService.right(Unit)

    userProcesses.checkAuthToken(
      sessionToken = SessionToken(mockEq(sessionToken.value)),
      androidId    = AndroidId(mockEq(androidId.value)),
      authToken    = mockEq(failingAuthToken),
      requestUri   = any[String]
    ) returns NineCardsService.right(userId)

    userProcesses.signUpUser(any[LoginRequest]) returns NineCardsService.right(Messages.loginResponse)

    userProcesses.updateInstallation(mockEq(Messages.updateInstallationRequest)) returns
      NineCardsService.right(Messages.updateInstallationResponse)

    rankingProcesses.getRanking(any) returns Free.pure(Either.right(Messages.rankings.getResponse))

    rankingProcesses.reloadRankingByScope(any, any) returns
      Free.pure(Either.right(Messages.rankings.reloadResponse))

  }

}

class NineCardsApiSpec
  extends NineCardsApiSpecification {

  private[this] def unauthorizedNoHeaders(request: HttpRequest) = {

    "return a 401 Unauthorized status code if no headers are provided" in new BasicScope {
      request ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "return a 401 Unauthorized status code if some of the headers aren't provided" in new BasicScope {
      request ~> addHeader(RawHeader(headerAndroidId, androidId.value)) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "return a 401 Unauthorized status code if a wrong credential is provided" in new UnsuccessfulScope {
      request ~> addHeaders(Headers.failingUserInfoHeaders) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "return a 401 Unauthorized status code if a persistence error happens" in new FailingScope {
      request ~> addHeaders(Headers.failingUserInfoHeaders) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }.pendingUntilFixed("Pending using EitherT")

  }

  private[this] def internalServerError(request: HttpRequest) = {
    "return 500 Internal Server Error status code if a persistence error happens" in new FailingScope {
      request ~> addHeaders(Headers.userInfoHeaders) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.InternalServerError.intValue
      }
    }.pendingUntilFixed("Pending using EitherT")
  }

  private[this] def badRequestEmptyBody(request: HttpRequest) = {
    "return a 400 BadRequest if no body is provided" in new BasicScope {
      request ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.BadRequest.intValue
      }
    }
  }

  private[this] def authenticatedBadRequestEmptyBody(request: HttpRequest) = {
    "return a 400 BadRequest if no body is provided" in new BasicScope {
      request ~> addHeaders(Headers.userInfoHeaders) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.BadRequest.intValue
      }
    }
  }

  private[this] def successOk(request: HttpRequest) = {
    "return a 200 OK Status code if the operation was carried out" in new SuccessfulScope {
      request ~> addHeaders(Headers.userInfoHeaders) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.OK.intValue
      }
    }
  }

  "nineCardsApi" should {
    "grant access to Swagger documentation" in new BasicScope {
      Get(Paths.apiDocs) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status should be equalTo StatusCodes.OK.intValue
          mediaType === MediaTypes.`text/html`
          responseAs[String] must contain("Swagger")
        }
    }

    "return a 404 NotFound error for an undefined path " in new BasicScope {
      Get(Paths.invalid) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.NotFound.intValue
      }
    }

  }
  "POST /login" should {

    val request = Post(Paths.login, Messages.apiLoginRequest)

    "return a 401 Unauthorized status code if the given email is empty" in new BasicScope {

      Post(Paths.login, Messages.apiLoginRequest.copy(email = Email(""))) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 401 Unauthorized status code if the given tokenId is empty" in new BasicScope {

      Post(Paths.login, Messages.apiLoginRequest.copy(tokenId = GoogleIdToken(""))) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 401 Unauthorized status code if the given email address and tokenId are not valid" in new UnsuccessfulScope {

      Post(Paths.login, Messages.apiLoginRequest) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 200 Ok status code if the given email address and tokenId are valid" in new SuccessfulScope {

      Post(Paths.login, Messages.apiLoginRequest) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.OK.intValue
        }
    }

    badRequestEmptyBody(Post(Paths.login))

    internalServerError(request)

  }

  "PUT /installations" should {

    val request = Put(Paths.installations, Messages.apiUpdateInstallationRequest)

    unauthorizedNoHeaders(request)

    authenticatedBadRequestEmptyBody(Put(Paths.installations))

    internalServerError(request)

    successOk(request)
  }

  "POST /widgets/rank" should {

    val request = Post(
      uri     = Paths.rankWidgets,
      content = Messages.apiRankByMomentsRequest
    )

    authenticatedBadRequestEmptyBody(Post(Paths.rankWidgets))

    unauthorizedNoHeaders(request)

    successOk(request)
  }

  "POST /recommendations" should {

    val request = Post(
      uri     = Paths.recommendationsForApps,
      content = Messages.apiGetRecommendationsForAppsRequest
    ) ~> addHeaders(Headers.googlePlayHeaders)

    authenticatedBadRequestEmptyBody(Post(Paths.recommendationsForApps))

    unauthorizedNoHeaders(request)

    successOk(request)
  }

  "POST /recommendations/category" should {

    val request = Post(
      uri     = Paths.recommendationsByCategory,
      content = Messages.apiGetRecommendationsByCategoryRequest
    ) ~> addHeaders(Headers.googlePlayHeaders)

    authenticatedBadRequestEmptyBody(Post(Paths.recommendationsByCategory))

    unauthorizedNoHeaders(request)

    successOk(request)
  }

  def testRanking(scopePath: String) = {
    val path = s"/rankings/$scopePath"

    s""" "GET ${path}", the endpoint to read a ranking,""" should {
      val request = Get(path)

      internalServerError(request)

      "return a 200 OK Status code if the operation was carried out" in new SuccessfulScope {
        request ~> sealRoute(nineCardsApi) ~> check {
          status.intValue shouldEqual StatusCodes.OK.intValue
        }
      }

    }

    s""" "POST $path", the endpoint to refresh an ranking,""" should {

      import NineCardsMarshallers._

      val request = Post(path, Messages.rankings.reloadApiRequest)

      "return a 200 OK Status code if the operation was carried out" in new SuccessfulScope {
        request ~> addHeaders(Headers.googleAnalyticsHeaders) ~> sealRoute(nineCardsApi) ~> check {
          status.intValue shouldEqual StatusCodes.OK.intValue
        }
      }
    }

  }

  val rankingPaths: List[String] = {
    val countries = List("countries/es", "countries/ES", "countries/gb", "countries/us", "countries/it")
    "world" :: countries
  }

  rankingPaths foreach testRanking

}
