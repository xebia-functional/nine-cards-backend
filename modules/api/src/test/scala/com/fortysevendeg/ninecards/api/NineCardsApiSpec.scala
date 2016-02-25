package com.fortysevendeg.ninecards.api

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.testkit._
import cats.free.Free
import com.fortysevendeg.ninecards.api.NineCardsHeaders._
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages.ApiUpdateInstallationRequest
import com.fortysevendeg.ninecards.api.messages.UserMessages.ApiLoginRequest
import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.processes.UserProcesses
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages.{LoginRequest, LoginResponse}
import com.fortysevendeg.ninecards.services.common.TaskOps._
import com.fortysevendeg.ninecards.services.persistence.PersistenceExceptions.PersistenceException
import org.mockito.Matchers.{eq => mockEq}
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes
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

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(5.second dilated system)

  implicit def actorRefFactory = system

  trait BasicScope extends Scope {

    implicit val userProcesses: UserProcesses[NineCardsServices] = mock[UserProcesses[NineCardsServices]]

    val nineCardsApi = new NineCardsApi {
      override implicit def actorRefFactory: ActorRefFactory = NineCardsApiSpecification.this.actorRefFactory
    }.nineCardsApiRoute

  }

  trait SuccessfulScope extends BasicScope {

    userProcesses.checkSessionToken(sessionToken, androidId) returns Free.pure(Option(userId))

    userProcesses.signUpUser(loginRequest) returns Free.pure(loginResponse)

    userProcesses.updateInstallation(
      mockEq(updateInstallationRequest))(
      any) returns Free.pure(updateInstallationResponse)
  }

  trait UnsuccessfulScope extends BasicScope {

    userProcesses.checkSessionToken(sessionToken, androidId) returns Free.pure(None)
  }

  trait FailingScope extends BasicScope {

    userProcesses.checkSessionToken(sessionToken, androidId) returns Free.pure(Option(userId))

    userProcesses.checkSessionToken(failingSessionToken, androidId) returns checkSessionTokenTask.liftF[NineCardsServices]

    userProcesses.signUpUser(loginRequest) returns loginTask.liftF[NineCardsServices]

    userProcesses.updateInstallation(
      mockEq(updateInstallationRequest))(
      any) returns updateInstallationTask.liftF[NineCardsServices]
  }

}

trait NineCardsApiContext {

  val loginPath = "/login"

  val installationsPath = "/installations"

  val apiDocsPath = "/apiDocs/index.html"

  val appId = "269b293b-d5f9-4f76-bbcd-020c3fc11336"

  val appKey = "f73d5513-16ac-4894-840d-08e8cf36b4b1"

  val androidId = "f07a13984f6d116a"

  val email = "valid.email@test.com"

  val oauthToken = "DQAAABQBAACJr1nBqQRTmbhS7yFG8NbWqkSXJchcJ5t8FEH-FNNtpk0cU-Xy8-nc_z4fuQV3Sw-INSFK_NuQnafoqNI06nHPD4yaqXVnQbonrVsokBKQnmkQ9SsD0jVZi8bUsC4ehd-w2tmEe7SZ_8vXhw_3f1iNnsrAqkpEvbPkFIo9oZeAq26us2dTo22Ttn3idGoua8Is_PO9EKzItDQD-0T9QXIDDl5otNMG5T4MS9vrbPOEhjorHqGfQJjT8Y10SK2QdgwwyIF2nCGZ6N-E-hbLjD0caXkY7ATpzhOUIJNnBitIs-h52E8JzgHysbYBK9cy6k6Im0WPyHvzXvrwsUK2RTwh-YBpFVSpBACmc89OZKnYE-VfgKHg9SSv1aNrBeEETQE"

  val sessionToken = "1d1afeea-c7ec-45d8-a6f8-825b836f2785"

  val failingSessionToken = "a439c00e-9a01-4b0e-a446-1d8410229072"

  val deviceToken = Option("d897b6f1-c6a9-42bd-bf42-c787883c7d3e")

  val userId = 1l

  val apiLoginRequest = ApiLoginRequest(email, androidId, oauthToken)

  val apiUpdateInstallationRequest = ApiUpdateInstallationRequest(deviceToken)

  val loginRequest = LoginRequest(email, androidId, oauthToken)

  val loginResponse = LoginResponse(sessionToken)

  val updateInstallationRequest = UpdateInstallationRequest(userId, androidId, deviceToken)

  val updateInstallationResponse = UpdateInstallationResponse(androidId, deviceToken)

  val persistenceException = PersistenceException(
    message = "Test error",
    cause = Option(new RuntimeException("Test error")))

  val checkSessionTokenTask: Task[Option[Long]] = Task.fail(persistenceException)

  val loginTask: Task[LoginResponse] = Task.fail(persistenceException)

  val updateInstallationTask: Task[UpdateInstallationResponse] = Task.fail(persistenceException)

  val apiRequestHeaders = List(
    RawHeader(headerAppId, appId),
    RawHeader(headerApiKey, appKey))

  val userInfoHeaders = List(
    RawHeader(headerAndroidId, androidId),
    RawHeader(headerSessionToken, sessionToken))

  val failingUserInfoHeaders = List(
    RawHeader(headerAndroidId, androidId),
    RawHeader(headerSessionToken, failingSessionToken))
}

class NineCardsApiSpec
  extends NineCardsApiSpecification {

  "nineCardsApi" should {
    "grant access to Swagger documentation" in new BasicScope {
      Get(apiDocsPath) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status should be equalTo 200
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
          status.intValue shouldEqual 401
        }
    }
    "return a 401 Unauthorized status code if some of the headers aren't provided" in new BasicScope {

      Post(loginPath) ~>
        addHeader(RawHeader(headerAppId, appId)) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }
    "return a 401 Unauthorized status code if the given email is empty" in new BasicScope {

      Post(loginPath, ApiLoginRequest("", androidId, oauthToken)) ~>
        addHeaders(apiRequestHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }
    "return a 401 Unauthorized status code if the given oauth token is empty" in new BasicScope {

      Post(loginPath, ApiLoginRequest(email, androidId, "")) ~>
        addHeaders(apiRequestHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }
    "return a 200 Ok status code if all the required info is provided" in new SuccessfulScope {

      Post(loginPath, apiLoginRequest) ~>
        addHeaders(apiRequestHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 200
        }
    }
    "return a 500 Internal Server Error status code if a persistence error happens" in new FailingScope {

      Post(loginPath, apiLoginRequest) ~>
        addHeaders(apiRequestHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 500
        }
    }
  }

  "PUT /installations" should {
    "return a 401 Unauthorized status code if no headers are provided" in new BasicScope {

      Put(installationsPath) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }

    "return a 401 Unauthorized status code if some of the headers aren't provided" in new BasicScope {

      Put(installationsPath) ~>
        addHeader(RawHeader(headerAndroidId, androidId)) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }

    "return a 401 Unauthorized status code if a wrong credential is provided" in new UnsuccessfulScope {

      Put(installationsPath) ~>
        addHeaders(userInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }

    "return a 401 Unauthorized status code if a persistence error happens" in new FailingScope {

      Put(installationsPath, apiUpdateInstallationRequest) ~>
        addHeaders(failingUserInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }

    "return a 200 Ok status code if a valid credential is provided" in new SuccessfulScope {

      Put(installationsPath, apiUpdateInstallationRequest) ~>
        addHeaders(userInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 200
        }
    }

    "return a 500 Internal Server Error status code if a persistence error happens" in new FailingScope {

      Put(installationsPath, apiUpdateInstallationRequest) ~>
        addHeaders(userInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 500
        }
    }
  }
}