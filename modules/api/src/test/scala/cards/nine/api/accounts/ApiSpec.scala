/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.api.accounts

import akka.actor.ActorSystem
import akka.testkit._
import cards.nine.api.{ AuthHeadersRejectionHandler, NineCardsExceptionHandler }
import cards.nine.api.NineCardsHeaders._
import cards.nine.api.accounts.TestData._
import cards.nine.commons.NineCardsErrors.{ AuthTokenNotValid, WrongEmailAccount }
import cards.nine.commons.NineCardsService
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.commons.config.NineCardsConfig
import cards.nine.domain.account._
import cards.nine.processes.NineCardsServices._
import cards.nine.processes._
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.account.messages._
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http.{ HttpRequest, StatusCodes }
import spray.httpx.SprayJsonSupport
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

import scala.concurrent.duration.DurationInt

trait AccountsApiSpecification
  extends Specification
  with AuthHeadersRejectionHandler
  with HttpService
  with JsonFormats
  with SprayJsonSupport
  with Matchers
  with Mockito
  with NineCardsExceptionHandler
  with Specs2RouteTest {

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(20.second dilated system)

  implicit def actorRefFactory = system

  trait BasicScope extends Scope {

    implicit val accountProcesses: AccountProcesses[NineCardsServices] = mock[AccountProcesses[NineCardsServices]]

    implicit val config: NineCardsConfiguration = NineCardsConfig.nineCardsConfiguration

    val routes = sealRoute(new AccountsApi().route)

    accountProcesses.checkAuthToken(
      sessionToken = SessionToken(mockEq(sessionToken.value)),
      androidId    = AndroidId(mockEq(androidId.value)),
      authToken    = mockEq(authToken),
      requestUri   = any[String]
    ) returns NineCardsService.right(userId)
  }

  trait SuccessfulScope extends BasicScope {

    accountProcesses.checkGoogleTokenId(email, tokenId) returns NineCardsService.right(Unit)

    accountProcesses.signUpUser(any[LoginRequest]) returns NineCardsService.right(Messages.loginResponse)

    accountProcesses.updateInstallation(mockEq(Messages.updateInstallationRequest)) returns
      NineCardsService.right(Messages.updateInstallationResponse)

  }

  trait UnsuccessfulScope extends BasicScope {

    accountProcesses.checkGoogleTokenId(email, tokenId) returns
      NineCardsService.left(WrongEmailAccount("The given email account is not valid"))

    accountProcesses.checkAuthToken(
      sessionToken = SessionToken(mockEq(sessionToken.value)),
      androidId    = AndroidId(mockEq(androidId.value)),
      authToken    = mockEq(failingAuthToken),
      requestUri   = any[String]
    ) returns NineCardsService.left(AuthTokenNotValid("The provided auth token is not valid"))

  }

  trait FailingScope extends BasicScope {

    accountProcesses.checkGoogleTokenId(email, tokenId) returns NineCardsService.right(Unit)

    accountProcesses.checkAuthToken(
      sessionToken = SessionToken(mockEq(sessionToken.value)),
      androidId    = AndroidId(mockEq(androidId.value)),
      authToken    = mockEq(failingAuthToken),
      requestUri   = any[String]
    ) returns NineCardsService.right(userId)

    accountProcesses.signUpUser(any[LoginRequest]) returns NineCardsService.right(Messages.loginResponse)

    accountProcesses.updateInstallation(mockEq(Messages.updateInstallationRequest)) returns
      NineCardsService.right(Messages.updateInstallationResponse)

  }

}

class AccountsApiSpec
  extends AccountsApiSpecification {

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

  "POST /login" should {

    val request = Post(Paths.login, Messages.apiLoginRequest)

    "return a 401 Unauthorized status code if the given email is empty" in new BasicScope {

      Post(Paths.login, Messages.apiLoginRequest.copy(email = Email(""))) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "return a 401 Unauthorized status code if the given tokenId is empty" in new BasicScope {

      Post(Paths.login, Messages.apiLoginRequest.copy(tokenId = GoogleIdToken(""))) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "return a 401 Unauthorized status code if the given email address and tokenId are not valid" in new UnsuccessfulScope {

      Post(Paths.login, Messages.apiLoginRequest) ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.Unauthorized.intValue
      }
    }

    "return a 200 Ok status code if the given email address and tokenId are valid" in new SuccessfulScope {

      Post(Paths.login, Messages.apiLoginRequest) ~> routes ~> check {
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

}
