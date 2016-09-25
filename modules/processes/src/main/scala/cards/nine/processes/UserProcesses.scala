package cards.nine.processes

import cards.nine.processes.converters.Converters._
import cards.nine.processes.messages.InstallationsMessages._
import cards.nine.processes.messages.UserMessages._
import cards.nine.processes.utils.HashUtils
import cards.nine.services.common.FreeUtils._
import cards.nine.services.common.NineCardsConfig
import cards.nine.services.free.algebra
import cards.nine.services.free.domain._
import cats.free.Free

class UserProcesses[F[_]](
  implicit
  userServices: algebra.User.Services[F],
  config: NineCardsConfig,
  hashUtils: HashUtils
) {

  val userNotFound: Option[Long] = None

  def signUpUser(loginRequest: LoginRequest): Free[F, LoginResponse] =
    userServices.getByEmail(loginRequest.email) flatMap {
      case Some(user) ⇒
        signUpInstallation(loginRequest.androidId, user)
      case None ⇒
        val apiKey = hashUtils.hashValue(loginRequest.sessionToken)

        for {
          newUser ← userServices.add(
            email        = loginRequest.email,
            apiKey       = apiKey,
            sessionToken = loginRequest.sessionToken
          )
          installation ← userServices.addInstallation(
            user        = newUser.id,
            deviceToken = None,
            androidId   = loginRequest.androidId
          )
        } yield (newUser, installation)
    } map toLoginResponse

  private def signUpInstallation(androidId: String, user: User) =
    userServices.getInstallationByUserAndAndroidId(user.id, androidId) flatMap {
      case Some(installation) ⇒
        (user, installation).toFree
      case None ⇒
        userServices.addInstallation(user.id, None, androidId) map {
          installation ⇒ (user, installation)
        }
    }

  def updateInstallation(request: UpdateInstallationRequest): Free[F, UpdateInstallationResponse] =
    userServices.updateInstallation(
      user        = request.userId,
      androidId   = request.androidId,
      deviceToken = request.deviceToken
    ) map toUpdateInstallationResponse

  def checkAuthToken(
    sessionToken: String,
    androidId: String,
    authToken: String,
    requestUri: String
  ): Free[F, Option[Long]] =
    userServices.getBySessionToken(sessionToken) flatMap {
      case Some(user) ⇒
        if (config.getOptionalBoolean("ninecards.backend.debugMode").getOrElse(false))
          Option(user.id).toFree
        else
          validateAuthToken(user, androidId, authToken, requestUri)
      case _ ⇒ userNotFound.toFree
    }

  private[this] def validateAuthToken(
    user: User,
    androidId: String,
    authToken: String,
    requestUri: String
  ) = {
    val expectedAuthToken = hashUtils.hashValue(
      text      = requestUri,
      secretKey = user.apiKey,
      salt      = None
    )

    if (expectedAuthToken.equals(authToken))
      userServices.getInstallationByUserAndAndroidId(
        user      = user.id,
        androidId = androidId
      ).map(_ map (_ ⇒ user.id))
    else
      userNotFound.toFree[F]
  }
}

object UserProcesses {

  implicit def processes[F[_]](
    implicit
    userServices: algebra.User.Services[F],
    config: NineCardsConfig,
    hashUtils: HashUtils
  ) = new UserProcesses

}
