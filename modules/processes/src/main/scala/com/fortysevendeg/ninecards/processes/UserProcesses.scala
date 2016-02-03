package com.fortysevendeg.ninecards.processes

import java.util.UUID

import cats.{Id, ~>}
import cats.free.{Inject, Free}
import com.fortysevendeg.ninecards.processes.messages.AddUserRequest
import com.fortysevendeg.ninecards.services.free.algebra.Users.{GetInstallation, GetUser, UserOps, UserServices}
import com.fortysevendeg.ninecards.processes.domain.User
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.services.free.domain.{User => UserAppServices, Installation => InstallationServices}
import scala.language.higherKinds


class UserProcesses[F[_]](
  implicit userServices: UserServices[F],
  I: Inject[UserOps, F]) {

  def signUpUser(userRequest: AddUserRequest): Free[F, User] =
    userServices.getUserByEmail(userRequest.email) flatMap {
      case Some(user) =>
        signUpInstallation(userRequest, user)
        Free.inject[UserOps, F](GetUser(user))
      case None =>
        for {
          newUser <- userServices.createUser(userRequest.email, userRequest.androidId, UUID.randomUUID.toString)
          installation <- userServices.createInstallation(newUser.id, userRequest.androidId)
        } yield newUser
    } map toUserApp

  private def signUpInstallation(data: AddUserRequest, user: UserAppServices): Free[F, InstallationServices] =
    userServices.getInstallationByAndroidId(data.androidId) flatMap {
      case Some(installation) => Free.inject[UserOps, F](GetInstallation(installation))
      case None => userServices.createInstallation(user.id, data.androidId)
    }
}

object UserProcesses {

  implicit def userProcesses[F[_]](implicit userServices: UserServices[F], inject: Inject[UserOps, F]) = new UserProcesses()

}
