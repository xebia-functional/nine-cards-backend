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
package cards.nine.api

import java.util.UUID

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.NineCardsHeaders._
import cards.nine.api.accounts.messages.ApiLoginRequest
import cards.nine.api.utils.AkkaHttpMatchers.PriceFilterSegment
import cards.nine.api.utils.TaskDirectives._
import cards.nine.api.utils.ScalazTaskUtils._
import cards.nine.commons.NineCardsService._
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.domain.account._
import cards.nine.domain.application.PriceFilter
import cards.nine.domain.market.{ Localization, MarketCredentials, MarketToken }
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.NineCardsServices._
import cards.nine.processes._
import cats.syntax.either._
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scalaz.concurrent.Task
import shapeless._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives._

class NineCardsDirectives(
  implicit
  accountProcesses: AccountProcesses[NineCardsServices],
  config: NineCardsConfiguration,
  ec: ExecutionContext
)
  extends BasicDirectives
  with HeaderDirectives
  with MarshallingDirectives
  with MiscDirectives
  with PathDirectives
  with RouteDirectives
  with SecurityDirectives
  with JsonFormats {

  import cards.nine.api.accounts.JsonFormats._

  implicit def fromTaskAuth[T](auth: ⇒ Task[AuthenticationResult[T]]): Future[AuthenticationResult[T]] =
    auth.unsafePerformAsyncFuture()

  val rejectionByCredentialsRejected = AuthenticationFailedRejection(
    cause            = AuthenticationFailedRejection.CredentialsRejected,
    challengeHeaders = Nil
  )

  val authenticateLoginRequest: Directive1[SessionToken] = for {
    request ← entity(as[ApiLoginRequest])
    _ ← authenticate(validateLoginRequest(request.email, request.tokenId))
    sessionToken ← generateSessionToken
  } yield sessionToken

  def validateLoginRequest(email: Email, tokenId: GoogleIdToken): Task[AuthenticationResult[Unit]] =
    if (email.value.isEmpty || tokenId.value.isEmpty)
      Task.now(Left(rejectionByCredentialsRejected))
    else
      accountProcesses
        .checkGoogleTokenId(email, tokenId)
        .leftMap(_ ⇒ rejectionByCredentialsRejected)
        .value
        .foldMap(prodInterpreters)
        .handle {
          case _ ⇒ Left(rejectionByCredentialsRejected)
        }

  val authenticateUser: Directive1[UserContext] = for {
    uri ← extractUri
    herokuForwardedProtocol ← optionalHeaderValueByName(headerHerokuForwardedProto)
    herokuUri = herokuForwardedProtocol.filterNot(_.isEmpty).fold(uri)(uri.withScheme)
    sessionToken ← headerValueByName(headerSessionToken).map(SessionToken)
    androidId ← headerValueByName(headerAndroidId).map(AndroidId)
    authToken ← headerValueByName(headerAuthToken)
    userId ← authenticate(validateUser(sessionToken, androidId, authToken, herokuUri))
  } yield UserContext(UserId(userId), androidId) :: HNil

  val marketAuthHeaders: Directive1[MarketCredentials] =
    for {
      androidId ← headerValueByName(headerAndroidId).map(AndroidId)
      marketToken ← headerValueByName(headerGooglePlayToken).map(MarketToken)
      localization ← optionalHeaderValueByName(headerMarketLocalization).map(_.map(Localization))
    } yield MarketCredentials(androidId, marketToken, localization)

  def validateUser(
    sessionToken: SessionToken,
    androidId: AndroidId,
    authToken: String,
    requestUri: Uri
  ): Task[AuthenticationResult[Long]] =
    accountProcesses.checkAuthToken(
      sessionToken = sessionToken,
      androidId    = androidId,
      authToken    = authToken,
      requestUri   = requestUri.toString
    ).foldMap(prodInterpreters) map { result ⇒
      //TODO: Provide more details about the cause of the rejection with a custom rejection handler
      result.leftMap(_ ⇒ rejectionByCredentialsRejected)
    } handle {
      case _ ⇒ Left(rejectionByCredentialsRejected)
    }

  val editorAuth: AsyncAuthenticator[String] = { credentials: Credentials ⇒
    def isEditor(cred: Credentials.Provided): Boolean =
      config.editors.get(cred.identifier).exists(cred.verify)

    credentials match {
      case c: Credentials.Provided if isEditor(c) ⇒ Future.successful(Some(c.identifier))
      case _ ⇒ Future.successful(None)
    }
  }

  val generateNewCollectionInfo: Directive1[NewSharedCollectionInfo] = for {
    currentDateTime ← provide(CurrentDateTime(DateTime.now))
    publicIdentifier ← provide(PublicIdentifier(UUID.randomUUID.toString))
  } yield NewSharedCollectionInfo(currentDateTime, publicIdentifier)

  val generateSessionToken: Directive1[SessionToken] = provide(SessionToken(UUID.randomUUID.toString))

  val priceFilterPath: Directive1[PriceFilter] =
    path(PriceFilterSegment) | (pathEndOrSingleSlash & provide(PriceFilter.ALL: PriceFilter))

}

object NineCardsDirectives {

  implicit def nineCardsDirectives(
    implicit
    accountProcesses: AccountProcesses[NineCardsServices],
    config: NineCardsConfiguration,
    ec: ExecutionContext
  ) = new NineCardsDirectives

}

