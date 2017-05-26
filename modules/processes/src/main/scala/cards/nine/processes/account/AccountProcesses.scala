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

import cards.nine.commons.NineCardsErrors.{ AuthTokenNotValid, InstallationNotFound, UserNotFound, WrongEmailAccount }
import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService.{ NineCardsService, Result }
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.domain.account._
import cards.nine.processes.utils.HashUtils
import cards.nine.services.free.algebra.{ GoogleApi, UserR }
import cards.nine.services.free.domain._
import freestyle.FreeS

class AccountProcesses[F[_]](
  implicit
  googleAPI: GoogleApi[F],
  userR: UserR[F],
  config: NineCardsConfiguration,
  hashUtils: HashUtils
) {

  import messages._
  import Converters._

  def toNCS[A](fs: FreeS.Par[F, Result[A]]): NineCardsService[F, A] = NineCardsService[F, A](fs.monad)

  def checkGoogleTokenId(email: Email, tokenId: GoogleIdToken): NineCardsService[F, Unit] =
    toNCS(googleAPI.getTokenInfo(tokenId))
      .ensure(WrongEmailAccount("The given email account is not valid")) { tokenInfo ⇒
        tokenInfo.email_verified == "true" && tokenInfo.email == email.value
      }
      .map(_ ⇒ Unit)

  def signUpUser(request: LoginRequest): NineCardsService[F, LoginResponse] = {

    def signupUserAndInstallation = {
      val apiKey = ApiKey(hashUtils.hashValue(request.sessionToken.value))

      for {
        user ← toNCS(userR.add(request.email, apiKey, request.sessionToken))
        installation ← toNCS(userR.addInstallation(user.id, deviceToken = None, androidId = request.androidId))
      } yield (user, installation)
    }

    def signUpInstallation(androidId: AndroidId, user: User) =
      toNCS(userR.getInstallationByUserAndAndroidId(user.id, androidId))
        .recoverWith {
          case _: InstallationNotFound ⇒ toNCS(userR.addInstallation(user.id, None, androidId))
        }

    val userInfo = for {
      u ← toNCS(userR.getByEmail(request.email))
      i ← signUpInstallation(request.androidId, u)
    } yield (u, i)

    userInfo
      .recoverWith {
        case _: UserNotFound ⇒ signupUserAndInstallation
      }
      .map(toLoginResponse)
  }

  def updateInstallation(request: UpdateInstallationRequest): NineCardsService[F, UpdateInstallationResponse] =
    toNCS(userR.updateInstallation(
      user        = request.userId,
      androidId   = request.androidId,
      deviceToken = request.deviceToken
    )).map(toUpdateInstallationResponse)

  def checkAuthToken(
    sessionToken: SessionToken,
    androidId: AndroidId,
    authToken: String,
    requestUri: String
  ): NineCardsService[F, Long] = {

    def validateAuthToken(user: User) = {
      val debugMode = config.debugMode.getOrElse(false)
      val expectedAuthToken = hashUtils.hashValue(requestUri, user.apiKey.value, None)

      if (debugMode || expectedAuthToken.equals(authToken))
        NineCardsService.right[F, Unit](Unit)
      else
        NineCardsService.left[F, Unit](AuthTokenNotValid("The provided auth token is not valid"))
    }

    for {
      user ← toNCS(userR.getBySessionToken(sessionToken))
      _ ← validateAuthToken(user)
      installation ← toNCS(userR.getInstallationByUserAndAndroidId(user.id, androidId))
    } yield user.id
  }

}

object AccountProcesses {

  implicit def processes[F[_]](
    implicit
    googleAPI: GoogleApi[F],
    userR: UserR[F],
    config: NineCardsConfiguration,
    hashUtils: HashUtils
  ) = new AccountProcesses

}
