package com.fortysevendeg.ninecards.processes

import java.util.UUID

import cats.free.Free
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages._
import com.fortysevendeg.ninecards.services.common.TaskOps._
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBOps
import com.fortysevendeg.ninecards.services.free.domain._
import com.fortysevendeg.ninecards.services.persistence.{UserPersistenceServices, _}
import doobie.imports._

import scala.language.higherKinds
import scalaz.Scalaz._

class UserProcesses[F[_]](
  implicit userPersistenceServices: UserPersistenceServices,
  dbOps: DBOps[F]) {
  def signUpUser(loginRequest: LoginRequest): Free[F, LoginResponse] = {
    userPersistenceServices.getUserByEmail(loginRequest.email) flatMap {
      case Some(user) =>
        signUpInstallation(loginRequest, user)
      case None =>
        for {
          newUser <- userPersistenceServices.addUser[User](loginRequest.email, UUID.randomUUID.toString)
          installation <- userPersistenceServices.createInstallation[Installation](
            userId = newUser.id,
            deviceToken = None,
            androidId = loginRequest.androidId)
        } yield (newUser, installation)
    }
  }.transact(transactor) map toLoginResponse

  private def signUpInstallation(request: LoginRequest, user: User): ConnectionIO[(User, Installation)] =
    userPersistenceServices.getInstallationByUserAndAndroidId(user.id, request.androidId) flatMap {
      case Some(installation) =>
        (user, installation).point[ConnectionIO]
      case None =>
        userPersistenceServices.createInstallation[Installation](user.id, None, request.androidId) map {
          installation => (user, installation)
        }
    }

  def updateInstallation(request: UpdateInstallationRequest): Free[F, UpdateInstallationResponse] =
    userPersistenceServices.updateInstallation(
      userId = request.userId,
      androidId = request.androidId,
      deviceToken = request.deviceToken).transact(transactor) map toUpdateInstallationResponse
}

object UserProcesses {

  implicit def userProcesses[F[_]](
    implicit userPersistenceServices: UserPersistenceServices, dbOps: DBOps[F]) = new UserProcesses()

}
