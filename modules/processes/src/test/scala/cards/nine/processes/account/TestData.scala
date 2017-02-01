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
package cards.nine.processes.account

import cards.nine.commons.NineCardsErrors._
import cards.nine.domain.account._
import cards.nine.services.free.domain.{ Installation, TokenInfo, User }
import com.roundeights.hasher.Hasher

private[account] trait AccountTestData {

  import messages._

  val email = Email("valid.email@test.com")

  val wrongEmail = Email("wrong.email@test.com")

  val tokenId = GoogleIdToken("JJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK")

  val tokenInfo = TokenInfo("true", email.value)

  val wrongEmailAccountError = WrongEmailAccount(message = "The given email account is not valid")

  val wrongAuthTokenError = WrongGoogleAuthToken(message = "Invalid Value")

  val userId = 1l

  val apiKey = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"

  val wrongApiKey = "cccccccc-cccc-cccc-cccc-cccccccccccc"

  val sessionToken = "dddddddd-dddd-dddd-dddd-dddddddddddd"

  val banned = false

  val user = User(userId, email, SessionToken(sessionToken), ApiKey(apiKey), banned)

  val userNotFoundError = UserNotFound("The user doesn't exist")

  val androidId = "aaaaaaaaaaaaaaaa"

  val googleTokenId = "eg-googleTokenId"

  val deviceToken = "abc"

  val installationId = 1l

  val loginRequest = LoginRequest(email, AndroidId(androidId), SessionToken(sessionToken), GoogleIdToken(googleTokenId))

  val loginResponse = LoginResponse(ApiKey(apiKey), SessionToken(sessionToken))

  val updateInstallationRequest = UpdateInstallationRequest(userId, AndroidId(androidId), Option(DeviceToken(deviceToken)))

  val updateInstallationResponse = UpdateInstallationResponse(AndroidId(androidId), Option(DeviceToken(deviceToken)))

  val installation = Installation(installationId, userId, Option(DeviceToken(deviceToken)), AndroidId(androidId))

  val installationNotFoundError = InstallationNotFound("The installation doesn't exist")

  val checkAuthTokenResponse = userId

  val dummyUrl = "http://localhost/dummy"

  val validAuthToken = Hasher(dummyUrl).hmac(apiKey).sha512.hex

  val wrongAuthToken = Hasher(dummyUrl).hmac(wrongApiKey).sha512.hex
  val authTokenNotValidError = AuthTokenNotValid("The provided auth token is not valid")

}

