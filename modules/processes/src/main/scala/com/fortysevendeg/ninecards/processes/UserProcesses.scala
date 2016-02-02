package com.fortysevendeg.ninecards.processes

import java.util.UUID

import cats.Id
import cats.free.{Inject, Free}
import com.fortysevendeg.ninecards.processes.messages.AddUserRequest
import com.fortysevendeg.ninecards.services.free.algebra.Users.UserServices
import com.fortysevendeg.ninecards.processes.domain.User
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.services.free.domain.{User => UserAppServices, Installation => InstallationServices}
import scala.language.higherKinds

class UserProcesses[F[_]](
  implicit userServices: UserServices[F],
  inject: Inject[Id, F]) {

  def signUpUser(userRequest: AddUserRequest): Free[F, User] =
    userServices.getUserByEmail(userRequest.email) flatMap {
      case Some(user) =>
        signUpDevice(userRequest, user)
        Free.inject[Id, F](user)
      case None =>
        for {
          newUser <- userServices.createUser(userRequest.email, userRequest.androidId, UUID.randomUUID.toString)
          device <- userServices.createInstallation(newUser.id, userRequest.androidId)
        } yield newUser
    } map toUserApp

  private def signUpDevice(data: AddUserRequest, user: UserAppServices): Free[F, InstallationServices] =
    userServices.getDeviceByAndroidId(data.androidId) flatMap {
      case Some(device) => Free.inject[Id, F](device)
      case None => userServices.createInstallation(user.id, data.androidId)
    }
}

object UserProcesses {

  implicit def userProcesses[F[_]](implicit userServices: UserServices[F],inject: Inject[Id, F]) = new UserProcesses()

}
