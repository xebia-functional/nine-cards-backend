package cards.nine.api.collections

import akka.actor.ActorSystem
import akka.testkit._
import cards.nine.api.{ AuthHeadersRejectionHandler, NineCardsExceptionHandler }
import cards.nine.api.NineCardsHeaders._
import cards.nine.api.TestData.{ Paths ⇒ _, Messages ⇒ _, _ }
import cards.nine.api.collections.TestData._
import cards.nine.commons.NineCardsErrors.AuthTokenNotValid
import cards.nine.commons.NineCardsService
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.commons.config.NineCardsConfig
import cards.nine.domain.account._
import cards.nine.processes.NineCardsServices._
import cards.nine.processes._
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.collections.SharedCollectionProcesses
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http.{ HttpRequest, StatusCodes }
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

import scala.concurrent.duration.DurationInt

trait CollectionsApiSpecification
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

    implicit val accountProcesses: AccountProcesses[NineCardsServices] = mock[AccountProcesses[NineCardsServices]]

    implicit val sharedCollectionProcesses: SharedCollectionProcesses[NineCardsServices] = mock[SharedCollectionProcesses[NineCardsServices]]

    implicit val config: NineCardsConfiguration = NineCardsConfig.nineCardsConfiguration

    val routes = sealRoute(new CollectionsApi().route)

    accountProcesses.checkAuthToken(
      sessionToken = SessionToken(mockEq(sessionToken.value)),
      androidId    = AndroidId(mockEq(androidId.value)),
      authToken    = mockEq(authToken),
      requestUri   = any[String]
    ) returns NineCardsService.right(userId)
  }

  trait SuccessfulScope extends BasicScope {

    sharedCollectionProcesses.createCollection(any) returns
      NineCardsService.right(Messages.createOrUpdateCollectionResponse)

    sharedCollectionProcesses.getCollectionByPublicIdentifier(any, any[String], any) returns
      NineCardsService.right(Messages.getCollectionByPublicIdentifierResponse)

    sharedCollectionProcesses.subscribe(any[String], any[Long]) returns
      NineCardsService.right(Messages.subscribeResponse)

    sharedCollectionProcesses.unsubscribe(any[String], any[Long]) returns
      NineCardsService.right(Messages.unsubscribeResponse)

    sharedCollectionProcesses.getLatestCollectionsByCategory(any, any, any, any) returns
      NineCardsService.right(Messages.getCollectionsResponse)

    sharedCollectionProcesses.getPublishedCollections(any[Long], any) returns
      NineCardsService.right(Messages.getCollectionsResponse)

    sharedCollectionProcesses.getSubscriptionsByUser(any) returns
      NineCardsService.right(Messages.getSubscriptionsByUserResponse)

    sharedCollectionProcesses.getTopCollectionsByCategory(any, any, any, any) returns
      NineCardsService.right(Messages.getCollectionsResponse)

    sharedCollectionProcesses.updateCollection(any, any, any) returns
      NineCardsService.right(Messages.createOrUpdateCollectionResponse)

    sharedCollectionProcesses.increaseViewsCountByOne(any) returns
      NineCardsService.right(Messages.increaseViewsCountByOneResponse)

  }

  trait UnsuccessfulScope extends BasicScope {

    accountProcesses.checkAuthToken(
      sessionToken = SessionToken(mockEq(sessionToken.value)),
      androidId    = AndroidId(mockEq(androidId.value)),
      authToken    = mockEq(failingAuthToken),
      requestUri   = any[String]
    ) returns NineCardsService.left(AuthTokenNotValid("The provided auth token is not valid"))

    sharedCollectionProcesses.getCollectionByPublicIdentifier(any, any[String], any) returns
      NineCardsService.left(sharedCollectionNotFoundError)

    sharedCollectionProcesses.subscribe(any[String], any[Long]) returns
      NineCardsService.left(sharedCollectionNotFoundError)

    sharedCollectionProcesses.unsubscribe(any[String], any[Long]) returns
      NineCardsService.left(sharedCollectionNotFoundError)

    sharedCollectionProcesses.updateCollection(any, any, any) returns
      NineCardsService.left(sharedCollectionNotFoundError)

    sharedCollectionProcesses.increaseViewsCountByOne(any) returns
      NineCardsService.left(sharedCollectionNotFoundError)
  }

  trait FailingScope extends BasicScope {

    accountProcesses.checkAuthToken(
      sessionToken = SessionToken(mockEq(sessionToken.value)),
      androidId    = AndroidId(mockEq(androidId.value)),
      authToken    = mockEq(failingAuthToken),
      requestUri   = any[String]
    ) returns NineCardsService.right(userId)

    sharedCollectionProcesses.createCollection(any) returns
      NineCardsService.right(Messages.createOrUpdateCollectionResponse)

    sharedCollectionProcesses.getCollectionByPublicIdentifier(any, any[String], any) returns
      NineCardsService.right(Messages.getCollectionByPublicIdentifierResponse)

    sharedCollectionProcesses.getLatestCollectionsByCategory(any, any, any, any) returns
      NineCardsService.right(Messages.getCollectionsResponse)

    sharedCollectionProcesses.getPublishedCollections(any[Long], any) returns
      NineCardsService.right(Messages.getCollectionsResponse)

    sharedCollectionProcesses.getSubscriptionsByUser(any) returns
      NineCardsService.right(Messages.getSubscriptionsByUserResponse)

    sharedCollectionProcesses.getTopCollectionsByCategory(any, any, any, any) returns
      NineCardsService.right(Messages.getCollectionsResponse)

    sharedCollectionProcesses.subscribe(any[String], any[Long]) returns
      NineCardsService.right(Messages.subscribeResponse)

    sharedCollectionProcesses.unsubscribe(any[String], any[Long]) returns
      NineCardsService.right(Messages.unsubscribeResponse)

    sharedCollectionProcesses.updateCollection(any, any, any) returns
      NineCardsService.right(Messages.createOrUpdateCollectionResponse)

    sharedCollectionProcesses.increaseViewsCountByOne(any) returns
      NineCardsService.right(Messages.increaseViewsCountByOneResponse)

  }

}

class CollectionsApiSpec extends CollectionsApiSpecification {

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

  private[this] def notFoundSharedCollection(request: HttpRequest) = {
    "return a 404 Not found status code if the shared collection doesn't exist" in new UnsuccessfulScope {
      request ~> addHeaders(Headers.userInfoHeaders) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.NotFound.intValue
      }
    }
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

  "POST /collections" should {

    val request = Post(Paths.collections, Messages.apiCreateCollectionRequest)

    unauthorizedNoHeaders(request)

    authenticatedBadRequestEmptyBody(Post(Paths.collections))

    internalServerError(request)

    successOk(request)
  }

  "GET /collections/collectionId" should {

    val request = Get(Paths.collectionById) ~> addHeaders(Headers.googlePlayHeaders)

    "return a 404 Not found status code if the shared collection doesn't exist" in new UnsuccessfulScope {

      Get(Paths.collectionById) ~>
        addHeaders(Headers.userInfoHeaders) ~>
        addHeaders(Headers.googlePlayHeaders) ~>
        routes ~>
        check {
          status.intValue shouldEqual StatusCodes.NotFound.intValue
        }
    }

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "PUT /collections/collectionId" should {

    val request = Put(Paths.collectionById, Messages.apiUpdateCollectionRequest)

    authenticatedBadRequestEmptyBody(Put(Paths.collectionById))

    notFoundSharedCollection(request)

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "POST /collections/collectionId/views" should {

    val request = Post(Paths.increaseViews)

    notFoundSharedCollection(request)

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "GET /collections/subscriptions" should {

    val request = Get(Paths.subscriptionsByUser)

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "PUT /collections/subscriptions/collectionId" should {

    val request = Put(Paths.subscriptionByCollectionId)

    unauthorizedNoHeaders(request)

    notFoundSharedCollection(request)

    internalServerError(request)

    successOk(request)
  }

  "DELETE /collections/subscriptions/collectionId" should {

    val request = Delete(Paths.subscriptionByCollectionId)

    unauthorizedNoHeaders(request)

    notFoundSharedCollection(request)

    internalServerError(request)

    successOk(request)
  }

  "GET /collections" should {

    val request = Get(Paths.collections) ~>
      addHeaders(Headers.googlePlayHeaders)

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "GET /collections/latest/category" should {

    val request = Get(Paths.latestCollections) ~>
      addHeaders(Headers.googlePlayHeaders)

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

  "GET /collections/top/category" should {

    val request = Get(Paths.topCollections) ~>
      addHeaders(Headers.googlePlayHeaders)

    unauthorizedNoHeaders(request)

    internalServerError(request)

    successOk(request)
  }

}
