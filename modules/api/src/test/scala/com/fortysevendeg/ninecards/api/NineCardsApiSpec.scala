package com.fortysevendeg.ninecards.api

import akka.actor.{ ActorRefFactory, ActorSystem }
import akka.testkit._
import cats.free.Free
import cats.syntax.xor._
import com.fortysevendeg.ninecards.api.NineCardsHeaders._
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages.ApiUpdateInstallationRequest
import com.fortysevendeg.ninecards.api.messages.UserMessages.ApiLoginRequest
import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.processes.ProcessesExceptions.SharedCollectionNotFoundException
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages._
import com.fortysevendeg.ninecards.processes._
import com.fortysevendeg.ninecards.services.common.TaskOps._
import com.fortysevendeg.ninecards.services.persistence.PersistenceExceptions.PersistenceException
import org.joda.time.DateTime
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http.{ MediaTypes, StatusCodes }
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

import scala.concurrent.duration.DurationInt
import scalaz.concurrent.Task

trait NineCardsApiSpecification
  extends Specification
  with AuthHeadersRejectionHandler
  with HttpService
  with JsonFormats
  with Matchers
  with Mockito
  with NineCardsApiContext
  with NineCardsExceptionHandler
  with Specs2RouteTest {

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(20.second dilated system)

  implicit def actorRefFactory = system

  trait BasicScope extends Scope {

    implicit val userProcesses: UserProcesses[NineCardsServices] = mock[UserProcesses[NineCardsServices]]

    implicit val googleApiProcesses: GoogleApiProcesses[NineCardsServices] = mock[GoogleApiProcesses[NineCardsServices]]

    implicit val sharedCollectionProcesses: SharedCollectionProcesses[NineCardsServices] = mock[SharedCollectionProcesses[NineCardsServices]]

    val nineCardsApi = new NineCardsApi {
      override implicit def actorRefFactory: ActorRefFactory = NineCardsApiSpecification.this.actorRefFactory
    }.nineCardsApiRoute

    userProcesses.checkAuthToken(
      sessionToken = mockEq(sessionToken),
      androidId    = mockEq(androidId),
      authToken    = mockEq(authToken),
      requestUri   = any[String]
    ) returns Free.pure(Option(userId))
  }

  trait SuccessfulScope extends BasicScope {

    googleApiProcesses.checkGoogleTokenId(email, tokenId) returns Free.pure(true)

    userProcesses.signUpUser(
      LoginRequest(email, androidId, tokenId, any)) returns Free.pure(loginResponse)

    userProcesses.updateInstallation(mockEq(updateInstallationRequest))(any) returns
      Free.pure(updateInstallationResponse)

    sharedCollectionProcesses.getCollectionByPublicIdentifier(any[String])(any) returns
      Free.pure(getCollectionByPublicIdentifierResponse.right)
  }

  trait UnsuccessfulScope extends BasicScope {

    googleApiProcesses.checkGoogleTokenId(email, tokenId) returns Free.pure(false)

    userProcesses.checkAuthToken(
      sessionToken = mockEq(sessionToken),
      androidId    = mockEq(androidId),
      authToken    = mockEq(failingAuthToken),
      requestUri   = any[String]
    ) returns Free.pure(None)

    sharedCollectionProcesses.getCollectionByPublicIdentifier(any[String])(any) returns
      Free.pure(sharedCollectionNotFoundException.left)
  }

  trait FailingScope extends BasicScope {

    googleApiProcesses.checkGoogleTokenId(email, tokenId) returns Free.pure(true)

    userProcesses.checkAuthToken(
      sessionToken = mockEq(sessionToken),
      androidId    = mockEq(androidId),
      authToken    = mockEq(failingAuthToken),
      requestUri   = any[String]
    ) returns checkAuthTokenTask.liftF[NineCardsServices]

    userProcesses.signUpUser(
      LoginRequest(email, androidId, tokenId, any)) returns loginTask.liftF[NineCardsServices]

    userProcesses.updateInstallation(mockEq(updateInstallationRequest))(any) returns
      updateInstallationTask.liftF[NineCardsServices]

    sharedCollectionProcesses.getCollectionByPublicIdentifier(any[String])(any) returns
      getCollectionByIdTask.liftF[NineCardsServices]
  }

}

trait NineCardsApiContext {

  val loginPath = "/login"

  val installationsPath = "/installations"

  val collectionByIdPath = "/collections/40daf308-fecf-4228-9262-a712d783cf49"

  val apiDocsPath = "/apiDocs/index.html"

  val appId = "269b293b-d5f9-4f76-bbcd-020c3fc11336"

  val appKey = "f73d5513-16ac-4894-840d-08e8cf36b4b1"

  val androidId = "f07a13984f6d116a"

  val email = "valid.email@test.com"

  val tokenId = "6c7b303e-585e-4fe8-8b6f-586547317331-7f9b12dd-8946-4285-a72a-746e482834dd"

  val apiToken = "a7db875d-f11e-4b0c-8d7a-db210fd93e1b"

  val authToken = "c8abd539-d912-4eff-8d3c-679307defc71"

  val failingAuthToken = "a439c00e-9a01-4b0e-a446-1d8410229072"

  val sessionToken = "1d1afeea-c7ec-45d8-a6f8-825b836f2785"

  val deviceToken = Option("d897b6f1-c6a9-42bd-bf42-c787883c7d3e")

  val userId = 1l

  val publicIdentifier = "40daf308-fecf-4228-9262-a712d783cf49"

  val collectionId = 1l

  val now = DateTime.now

  val description = Option("Description about the collection")

  val author = "John Doe"

  val name = "The best social media apps"

  val installations = 1

  val views = 1

  val category = "SOCIAL"

  val icon = "path-to-icon"

  val community = true

  val sharedLink = ""

  val apiLoginRequest = ApiLoginRequest(email, androidId, tokenId)

  val apiUpdateInstallationRequest = ApiUpdateInstallationRequest(deviceToken)

  val loginResponse = LoginResponse(apiToken, sessionToken)

  val updateInstallationRequest = UpdateInstallationRequest(userId, androidId, deviceToken)

  val updateInstallationResponse = UpdateInstallationResponse(androidId, deviceToken)

  val sharedCollectionInfo = SharedCollectionInfo(
    publicIdentifier = publicIdentifier,
    publishedOn      = now,
    description      = description,
    author           = author,
    name             = name,
    sharedLink       = sharedLink,
    installations    = installations,
    views            = views,
    category         = category,
    icon             = icon,
    community        = community,
    packages         = List.empty,
    resolvedPackages = List.empty
  )

  val getCollectionByPublicIdentifierResponse = GetCollectionByPublicIdentifierResponse(
    data = sharedCollectionInfo
  )

  val persistenceException = PersistenceException(
    message = "Test error",
    cause   = Option(new RuntimeException("Test error"))
  )

  val sharedCollectionNotFoundException = SharedCollectionNotFoundException(
    message = "Test error",
    cause   = Option(new RuntimeException("Test error"))
  )

  val checkAuthTokenTask: Task[Option[Long]] = Task.fail(persistenceException)

  val loginTask: Task[LoginResponse] = Task.fail(persistenceException)

  val updateInstallationTask: Task[UpdateInstallationResponse] = Task.fail(persistenceException)

  val getCollectionByIdTask: Task[XorGetCollectionByPublicId] = Task.now(persistenceException.left)

  val apiRequestHeaders = List(
    RawHeader(headerAppId, appId),
    RawHeader(headerApiKey, appKey)
  )

  val userInfoHeaders = List(
    RawHeader(headerAndroidId, androidId),
    RawHeader(headerSessionToken, sessionToken),
    RawHeader(headerAuthToken, authToken)
  )

  val failingUserInfoHeaders = List(
    RawHeader(headerAndroidId, androidId),
    RawHeader(headerSessionToken, sessionToken),
    RawHeader(headerAuthToken, failingAuthToken)
  )
}

class NineCardsApiSpec
  extends NineCardsApiSpecification {

  "nineCardsApi" should {
    "grant access to Swagger documentation" in new BasicScope {
      Get(apiDocsPath) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status should be equalTo StatusCodes.OK.intValue
          mediaType === MediaTypes.`text/html`
          responseAs[String] must contain("Swagger")
        }
    }
  }

  "POST /login" should {
    "return a 401 Unauthorized status code if no headers are provided" in new BasicScope {

      Post(loginPath) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }
    "return a 401 Unauthorized status code if some of the headers aren't provided" in new BasicScope {

      Post(loginPath) ~>
        addHeader(RawHeader(headerAppId, appId)) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }
    "return a 401 Unauthorized status code if the given email is empty" in new BasicScope {

      Post(loginPath, ApiLoginRequest("", androidId, tokenId)) ~>
        addHeaders(apiRequestHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }
    "return a 401 Unauthorized status code if the given tokenId is empty" in new BasicScope {

      Post(loginPath, ApiLoginRequest(email, androidId, "")) ~>
        addHeaders(apiRequestHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }
    "return a 401 Unauthorized status code if the given email address and tokenId are not valid" in new UnsuccessfulScope {

      Post(loginPath, apiLoginRequest) ~>
        addHeaders(apiRequestHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }
    "return a 200 Ok status code if the given email address and tokenId are valid" in new SuccessfulScope {

      Post(loginPath, apiLoginRequest) ~>
        addHeaders(apiRequestHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.OK.intValue
        }
    }
    "return a 500 Internal Server Error status code if a persistence error happens" in new FailingScope {

      Post(loginPath, apiLoginRequest) ~>
        addHeaders(apiRequestHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.InternalServerError.intValue
        }
    }
  }

  "PUT /installations" should {
    "return a 401 Unauthorized status code if no headers are provided" in new BasicScope {

      Put(installationsPath) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 401 Unauthorized status code if some of the headers aren't provided" in new BasicScope {

      Put(installationsPath) ~>
        addHeader(RawHeader(headerAndroidId, androidId)) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 401 Unauthorized status code if a wrong credential is provided" in new UnsuccessfulScope {

      Put(installationsPath) ~>
        addHeaders(failingUserInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 401 Unauthorized status code if a persistence error happens" in new FailingScope {

      Put(installationsPath, apiUpdateInstallationRequest) ~>
        addHeaders(failingUserInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 200 Ok status code if a valid credential is provided" in new SuccessfulScope {

      Put(installationsPath, apiUpdateInstallationRequest) ~>
        addHeaders(userInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.OK.intValue
        }
    }

    "return a 500 Internal Server Error status code if a persistence error happens" in new FailingScope {

      Put(installationsPath, apiUpdateInstallationRequest) ~>
        addHeaders(userInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.InternalServerError.intValue
        }
    }
  }

  "GET /collections/collectionId" should {
    "return a 401 Unauthorized status code if no headers are provided" in new BasicScope {

      Get(collectionByIdPath) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 401 Unauthorized status code if some of the headers aren't provided" in new BasicScope {

      Get(collectionByIdPath) ~>
        addHeader(RawHeader(headerAndroidId, androidId)) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 401 Unauthorized status code if a wrong credential is provided" in new UnsuccessfulScope {

      Get(collectionByIdPath) ~>
        addHeaders(failingUserInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 401 Unauthorized status code if a persistence error happens" in new FailingScope {

      Get(collectionByIdPath) ~>
        addHeaders(failingUserInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.Unauthorized.intValue
        }
    }

    "return a 200 Ok status code if a valid credential is provided" in new SuccessfulScope {

      Get(collectionByIdPath) ~>
        addHeaders(userInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.OK.intValue
        }
    }

    "return a 404 Not found status code if the shared collection doesn't exist" in new UnsuccessfulScope {

      Get(collectionByIdPath) ~>
        addHeaders(userInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.NotFound.intValue
        }
    }

    "return a 500 Internal Server Error status code if a persistence error happens" in new FailingScope {

      Get(collectionByIdPath) ~>
        addHeaders(userInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual StatusCodes.InternalServerError.intValue
        }
    }
  }
}