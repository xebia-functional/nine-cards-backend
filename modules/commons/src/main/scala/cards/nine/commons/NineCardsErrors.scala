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
package cards.nine.commons

object NineCardsErrors {

  sealed abstract class NineCardsError extends Serializable with Product

  final case class AuthTokenNotValid(message: String) extends NineCardsError

  final case class CountryNotFound(message: String) extends NineCardsError

  final case class FirebaseServerError(message: String) extends NineCardsError

  final case class GoogleAnalyticsServerError(message: String) extends NineCardsError

  final case class GoogleOAuthError(message: String) extends NineCardsError

  final case class HttpBadRequest(message: String) extends NineCardsError

  final case class HttpNotFound(message: String) extends NineCardsError

  final case class HttpUnauthorized(message: String) extends NineCardsError

  final case class InstallationNotFound(message: String) extends NineCardsError

  final case class PackageNotResolved(message: String) extends NineCardsError

  final case class RankingNotFound(message: String) extends NineCardsError

  final case class RecommendationsServerError(message: String) extends NineCardsError

  final case class ReportNotFound(message: String) extends NineCardsError

  final case class SharedCollectionNotFound(message: String) extends NineCardsError

  final case class UserNotFound(message: String) extends NineCardsError

  final case class WrongEmailAccount(message: String) extends NineCardsError

  final case class WrongGoogleAuthToken(message: String) extends NineCardsError
}
