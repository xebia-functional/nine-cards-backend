package com.fortysevendeg.ninecards.processes

import java.util.UUID

import cats.free.Free
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.domain.User
import com.fortysevendeg.ninecards.processes.messages.AddUserRequest
import com.fortysevendeg.ninecards.processes.messages.DevicesMessages.{UpdateDeviceRequest, UpdateDeviceResponse}
import com.fortysevendeg.ninecards.services.free.algebra.Users.UserServices
import com.fortysevendeg.ninecards.services.free.domain.{User => UserAppServices}

import scala.language.higherKinds

class UserProcesses[F[_]](
  implicit userServices: UserServices[F]) {

  def signUpUser(userRequest: AddUserRequest): Free[F, User] = for {
    maybeUser <- userServices.getUserByEmail(userRequest.email)
    user <- createOrReturnUser(maybeUser, userRequest)
  } yield toUserApp(user)

  private def createOrReturnUser(maybeUser: Option[UserAppServices], data: AddUserRequest): Free[F, UserAppServices] =
    maybeUser match {
      case Some(user) => userServices.addUser(user)
      case None => userServices.addUser(createFromGoogle(data))
    }

  private def createFromGoogle(addUserRequest: AddUserRequest): UserAppServices =
    UserAppServices(
      sessionToken = Option(UUID.randomUUID().toString))

  def updateDevice(request: UpdateDeviceRequest): Free[F, UpdateDeviceResponse] = for {
    updatedDevice <- userServices.updateDevice(
      userId = request.userId,
      androidId = request.androidId,
      deviceToken = request.deviceToken)
  } yield toUpdateDeviceResponse(updatedDevice)

}

object UserProcesses {

  implicit def userProcesses[F[_]](implicit userServices: UserServices[F]) = new UserProcesses()

}
