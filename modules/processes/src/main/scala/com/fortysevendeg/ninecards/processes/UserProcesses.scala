package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages._
import com.fortysevendeg.ninecards.processes.utils.HashUtils
import com.fortysevendeg.ninecards.services.common.ConnectionIOOps._
import com.fortysevendeg.ninecards.services.common.NineCardsConfig
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBOps
import com.fortysevendeg.ninecards.services.free.domain._
import com.fortysevendeg.ninecards.services.persistence.{ UserPersistenceServices, _ }
import doobie.imports._

import scalaz.Scalaz._
import scalaz.concurrent.Task

class UserProcesses[F[_]](
  implicit
  userPersistenceServices: UserPersistenceServices,
  config: NineCardsConfig,
  hashUtils: HashUtils,
  transactor: Transactor[Task],
  dbOps: DBOps[F]
) {

  val userNotFound: Option[Long] = None

  def signUpUser(loginRequest: LoginRequest): Free[F, LoginResponse] = {
    userPersistenceServices.getUserByEmail(loginRequest.email) flatMap {
      case Some(user) ⇒
        signUpInstallation(loginRequest.androidId, user)
      case None ⇒
        val apiKey = hashUtils.hashValue(loginRequest.sessionToken)

        for {
          newUser ← userPersistenceServices.addUser[User](
            email        = loginRequest.email,
            apiKey       = apiKey,
            sessionToken = loginRequest.sessionToken
          )
          installation ← userPersistenceServices.createInstallation[Installation](
            userId      = newUser.id,
            deviceToken = None,
            androidId   = loginRequest.androidId
          )
        } yield (newUser, installation)
    }
  }.liftF[F] map toLoginResponse

  private def signUpInstallation(androidId: String, user: User) =
    userPersistenceServices.getInstallationByUserAndAndroidId(user.id, androidId) flatMap {
      case Some(installation) ⇒
        (user, installation).point[ConnectionIO]
      case None ⇒
        userPersistenceServices.createInstallation[Installation](user.id, None, androidId) map {
          installation ⇒ (user, installation)
        }
    }

  def updateInstallation(
    request: UpdateInstallationRequest
  )(
    implicit
    ev: Composite[Installation]
  ): Free[F, UpdateInstallationResponse] = {
    val result = userPersistenceServices.updateInstallation[Installation](
      userId      = request.userId,
      androidId   = request.androidId,
      deviceToken = request.deviceToken
    )
    result.liftF[F] map toUpdateInstallationResponse
  }

  def checkAuthToken(
    sessionToken: String,
    androidId: String,
    authToken: String,
    requestUri: String
  ): Free[F, Option[Long]] = {
    userPersistenceServices.getUserBySessionToken(sessionToken) flatMap {
      case Some(user) ⇒
        if (config.getOptionalBoolean("ninecards.backend.debugMode").getOrElse(false))
          Option(user.id).point[ConnectionIO]
        else
          validateAuthToken(user, androidId, authToken, requestUri)
      case _ ⇒ userNotFound.point[ConnectionIO]
    }
  }.liftF[F]

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
      userPersistenceServices.getInstallationByUserAndAndroidId(
        userId    = user.id,
        androidId = androidId
      ).map(_ map (_ ⇒ user.id))
    else
      userNotFound.point[ConnectionIO]
  }
}

object UserProcesses {

  implicit def userProcesses[F[_]](
    implicit
    userPersistenceServices: UserPersistenceServices,
    config: NineCardsConfig,
    hashUtils: HashUtils,
    dbOps: DBOps[F]
  ) = new UserProcesses

}
