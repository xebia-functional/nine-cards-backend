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

import akka.http.scaladsl.marshalling.{ Marshaller, Marshalling }
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse }
import cards.nine.commons.NineCardsErrors._
import akka.http.scaladsl.model.StatusCodes._

import scala.concurrent.{ ExecutionContext, Future }

class NineCardsErrorHandler {

  def handleNineCardsErrors(e: NineCardsError)(implicit ctx: ExecutionContext): Future[List[Marshalling[HttpResponse]]] = {
    val (statusCode, errorMessage) = e match {
      case AuthTokenNotValid(message) ⇒
        (Unauthorized, message)
      case CountryNotFound(message) ⇒
        (NotFound, message)
      case FirebaseServerError(message) ⇒
        (ServiceUnavailable, message)
      case GoogleAnalyticsServerError(message) ⇒
        (ServiceUnavailable, message)
      case GoogleOAuthError(message) ⇒
        (Unauthorized, message)
      case HttpBadRequest(message) ⇒
        (BadRequest, message)
      case HttpNotFound(message) ⇒
        (NotFound, message)
      case HttpUnauthorized(message) ⇒
        (Unauthorized, message)
      case InstallationNotFound(message) ⇒
        (Unauthorized, message)
      case PackageNotResolved(message) ⇒
        (NotFound, message)
      case RankingNotFound(message) ⇒
        (NotFound, message)
      case RecommendationsServerError(message) ⇒
        (ServiceUnavailable, message)
      case ReportNotFound(message) ⇒
        (NotFound, message)
      case SharedCollectionNotFound(message) ⇒
        (NotFound, message)
      case UserNotFound(message) ⇒
        (Unauthorized, message)
      case WrongEmailAccount(message) ⇒
        (Unauthorized, message)
      case WrongGoogleAuthToken(message) ⇒
        (Unauthorized, message)
    }

    Marshaller.fromResponse(
      HttpResponse(status = statusCode, entity = HttpEntity(errorMessage))
    )
  }
}

object NineCardsErrorHandler {
  implicit val handler: NineCardsErrorHandler = new NineCardsErrorHandler
}
