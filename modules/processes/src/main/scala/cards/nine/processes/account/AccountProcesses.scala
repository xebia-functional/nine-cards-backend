package cards.nine.processes.account

import cards.nine.commons.NineCardsErrors.{ AuthTokenNotValid, InstallationNotFound, UserNotFound, WrongEmailAccount }
import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService.NineCardsService
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.domain.account._
import cards.nine.processes.utils.HashUtils
import cards.nine.services.free.algebra
import cards.nine.services.free.algebra.GoogleApi
import cards.nine.services.free.domain._

class AccountProcesses[F[_]](
  implicit
  googleAPIServices: algebra.GoogleApi.Services[F],
  userServices: algebra.User.Services[F],
  config: NineCardsConfiguration,
  hashUtils: HashUtils
) {

  import messages._
  import Converters._

  def checkGoogleTokenId(email: Email, tokenId: GoogleIdToken): NineCardsService[F, Unit] =
    googleAPIServices
      .getTokenInfo(tokenId)
      .ensure(WrongEmailAccount("The given email account is not valid")) { tokenInfo ⇒
        tokenInfo.email_verified == "true" && tokenInfo.email == email.value
      }
      .map(_ ⇒ Unit)

  def signUpUser(request: LoginRequest): NineCardsService[F, LoginResponse] = {

    def signupUserAndInstallation = {
      val apiKey = ApiKey(hashUtils.hashValue(request.sessionToken.value))

      for {
        user ← userServices.add(request.email, apiKey, request.sessionToken)
        installation ← userServices.addInstallation(user.id, deviceToken = None, androidId = request.androidId)
      } yield (user, installation)
    }

    def signUpInstallation(androidId: AndroidId, user: User) =
      userServices.getInstallationByUserAndAndroidId(user.id, androidId)
        .recoverWith {
          case _: InstallationNotFound ⇒ userServices.addInstallation(user.id, None, androidId)
        }

    val userInfo = for {
      u ← userServices.getByEmail(request.email)
      i ← signUpInstallation(request.androidId, u)
    } yield (u, i)

    userInfo
      .recoverWith {
        case _: UserNotFound ⇒ signupUserAndInstallation
      }
      .map(toLoginResponse)
  }

  def updateInstallation(request: UpdateInstallationRequest): NineCardsService[F, UpdateInstallationResponse] =
    userServices.updateInstallation(
      user        = request.userId,
      androidId   = request.androidId,
      deviceToken = request.deviceToken
    ).map(toUpdateInstallationResponse)

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
      user ← userServices.getBySessionToken(sessionToken)
      _ ← validateAuthToken(user)
      installation ← userServices.getInstallationByUserAndAndroidId(user.id, androidId)
    } yield user.id
  }

}

object AccountProcesses {

  implicit def processes[F[_]](
    implicit
    googleAPIServices: GoogleApi.Services[F],
    userServices: algebra.User.Services[F],
    config: NineCardsConfiguration,
    hashUtils: HashUtils
  ) = new AccountProcesses

}
