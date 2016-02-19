package com.fortysevendeg.ninecards.api

import java.util.UUID

import akka.actor.ActorRefFactory
import cats.free.Free
import com.fortysevendeg.ninecards.api.NineCardsHeaders._
import com.fortysevendeg.ninecards.api.messages.UserMessages.ApiLoginRequest
import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.processes.UserProcesses
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

trait NineCardsApiSpecification
  extends Specification
    with AuthHeadersRejectionHandler
    with HttpService
    with JsonFormats
    with Matchers
    with Mockito
    with NineCardsApiContext
    with Specs2RouteTest {

  val loginPath = "/login"
  val installationsPath = "/installations"
  val apiDocsPath = "/apiDocs/index.html"

  implicit val userProcesses: UserProcesses[NineCardsServices] = mock[UserProcesses[NineCardsServices]]

  implicit def actorRefFactory = system

  val nineCardsApi = new NineCardsApi {
    override implicit def actorRefFactory: ActorRefFactory = NineCardsApiSpecification.this.actorRefFactory
  }.nineCardsApiRoute

  userProcesses.checkSessionToken(sessionTokenHeaderValue, androidIdHeaderValue) returns Free.pure(Option(1l))

  userProcesses.checkSessionToken(wrongSessionTokenHeaderValue, androidIdHeaderValue) returns Free.pure(None)
}

trait NineCardsApiContext {

  val appIdHeaderValue = "269b293b-d5f9-4f76-bbcd-020c3fc11336"

  val appKeyHeaderValue = "f73d5513-16ac-4894-840d-08e8cf36b4b1"

  val androidIdHeaderValue = "f07a13984f6d116a"

  val emailValue = "x@y.com"

  val oauthTokenValue = "DQAAABQBAACJr1nBqQRTmbhS7yFG8NbWqkSXJchcJ5t8FEH-FNNtpk0cU-Xy8-nc_z4fuQV3Sw-INSFK_NuQnafoqNI06nHPD4yaqXVnQbonrVsokBKQnmkQ9SsD0jVZi8bUsC4ehd-w2tmEe7SZ_8vXhw_3f1iNnsrAqkpEvbPkFIo9oZeAq26us2dTo22Ttn3idGoua8Is_PO9EKzItDQD-0T9QXIDDl5otNMG5T4MS9vrbPOEhjorHqGfQJjT8Y10SK2QdgwwyIF2nCGZ6N-E-hbLjD0caXkY7ATpzhOUIJNnBitIs-h52E8JzgHysbYBK9cy6k6Im0WPyHvzXvrwsUK2RTwh-YBpFVSpBACmc89OZKnYE-VfgKHg9SSv1aNrBeEETQE"

  val sessionTokenHeaderValue = "1d1afeea-c7ec-45d8-a6f8-825b836f2785"

  val wrongSessionTokenHeaderValue = "52a6510c-d357-4bca-a7e7-49851c4910ec"

  val apiRequestHeaders = List(
    RawHeader(headerAppId, appIdHeaderValue),
    RawHeader(headerApiKey, appKeyHeaderValue))

  val validUserInfoHeaders = List(
    RawHeader(headerAndroidId, androidIdHeaderValue),
    RawHeader(headerSessionToken, sessionTokenHeaderValue))

  val wrongUserInfoHeaders = List(
    RawHeader(headerAndroidId, androidIdHeaderValue),
    RawHeader(headerSessionToken, wrongSessionTokenHeaderValue))
}

class NineCardsApiSpec
  extends NineCardsApiSpecification {

  "nineCardsApi" should {
    "grant access to Swagger documentation" in {
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
    "return a 401 Unauthorized status code if no headers are provided" in {

      Put(loginPath) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }
    "return a 401 Unauthorized status code if some of the headers aren't provided" in {

      Put(loginPath) ~>
        addHeader(RawHeader(headerAppId, appIdHeaderValue)) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }
    "return a 401 Unauthorized status code if the given email is empty" in {

      Put(loginPath, ApiLoginRequest("", androidIdHeaderValue, oauthTokenValue)) ~>
        addHeaders(apiRequestHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }
    "return a 401 Unauthorized status code if the given oauth token is empty" in {

      Put(loginPath, ApiLoginRequest(emailValue, androidIdHeaderValue, "")) ~>
        addHeaders(apiRequestHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }
    "return a code different than 401 Unauthorized status if all the required info is provided" in {

      Put(loginPath, ApiLoginRequest(emailValue, androidIdHeaderValue, oauthTokenValue)) ~>
        addHeaders(apiRequestHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldNotEqual 401
        }
    }
  }

  "PUT /installations" should {
    "return a 401 Unauthorized status code if no headers are provided" in {

      Put(installationsPath) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }

    "return a 401 Unauthorized status code if some of the headers aren't provided" in {

      Put(installationsPath) ~>
        addHeader(RawHeader(headerAndroidId, androidIdHeaderValue)) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }

    "return a 401 Unauthorized status code if a wrong credential is provided" in {

      Put(installationsPath) ~>
        addHeaders(wrongUserInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldEqual 401
        }
    }

    "return a code different than 401 Unauthorized status if a valid credential is provided" in {

      Put(installationsPath) ~>
        addHeaders(validUserInfoHeaders) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status.intValue shouldNotEqual 401
        }
    }
  }
}