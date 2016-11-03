package cards.nine.processes

import cards.nine.commons.NineCardsErrors.{ AuthTokenNotValid, InstallationNotFound, UserNotFound }
import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.domain.account._
import cards.nine.processes.converters.Converters._
import cards.nine.processes.messages.InstallationsMessages._
import cards.nine.processes.messages.UserMessages._
import cards.nine.processes.utils.HashUtils
import cards.nine.services.free.algebra
import cards.nine.services.free.domain._
import cats.free.Free

class UserProcesses[F[_]](
  implicit
  userServices: algebra.User.Services[F],
  config: NineCardsConfiguration,
  hashUtils: HashUtils
) {

  def signUpUser(request: LoginRequest): Free[F, Result[LoginResponse]] = {

    def signupUserAndInstallation(request: LoginRequest) = {
      val apiKey = ApiKey(hashUtils.hashValue(request.sessionToken.value))

      for {
        user ← userServices.add(request.email, apiKey, request.sessionToken)
        installation ← userServices.addInstallation(user.id, deviceToken = None, androidId = request.androidId)
      } yield (user, installation)
    } map toLoginResponse

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
      .map(toLoginResponse)
      .recoverWith {
        case _: UserNotFound ⇒ signupUserAndInstallation(request)
      }
      .value
  }

  def updateInstallation(request: UpdateInstallationRequest): Free[F, Result[UpdateInstallationResponse]] =
    userServices.updateInstallation(
      user        = request.userId,
      androidId   = request.androidId,
      deviceToken = request.deviceToken
    ).map(toUpdateInstallationResponse).value

  def checkAuthToken(
    sessionToken: SessionToken,
    androidId: AndroidId,
    authToken: String,
    requestUri: String
  ): Free[F, Result[Long]] = {

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
  }.value

}

object UserProcesses {

  implicit def processes[F[_]](
    implicit
    userServices: algebra.User.Services[F],
    config: NineCardsConfiguration,
    hashUtils: HashUtils
  ) = new UserProcesses

}
