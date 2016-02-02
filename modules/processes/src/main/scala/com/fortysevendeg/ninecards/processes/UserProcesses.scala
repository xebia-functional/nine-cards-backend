package com.fortysevendeg.ninecards.processes

import java.util.UUID

import cats.free.Free
import com.fortysevendeg.ninecards.processes.messages.{InstallationRequest, AddUserRequest}
import com.fortysevendeg.ninecards.services.free.algebra.Users.UserServices
import com.fortysevendeg.ninecards.processes.domain.{Installation, User}
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.services.free.domain.{User => UserAppServices, Device => DeviceServices}
import scala.language.higherKinds

class UserProcesses[F[_]](
  implicit userServices: UserServices[F]) {

  def signUpUser(userRequest: AddUserRequest): Free[F, User] = for {
    maybeUser <- userServices.getUserByEmail(userRequest.email)
    user <- createOrReturnUser(maybeUser, userRequest)
  } yield toUserApp(user)

  private def createOrReturnUser(maybeUser: Option[UserAppServices], data: AddUserRequest): Free[F,UserAppServices] =
    maybeUser match {
      case Some(user) => {
        signUpDevice(data, user)
        userServices.getUser(user)
      }
      case None => for {
        newUser <- userServices.createUser(data.email, data.androidId, UUID.randomUUID().toString)
        device <- userServices.createDevice(654, data.androidId)
      } yield newUser
    }

  private def signUpDevice(data: AddUserRequest, user: UserAppServices): Free[F,DeviceServices]= for {
    maybeDevice <- userServices.getDeviceByAndroidId(data.androidId)
    device <- createOrReturnDevice(maybeDevice, data, user)
  } yield device

  private def createOrReturnDevice(maybeDevice: Option[DeviceServices], data: AddUserRequest, user: UserAppServices): Free[F,DeviceServices] =
    maybeDevice match {
      case Some(device) => userServices.getDevice(device)
      case None => {
        userServices.createDevice(551, data.androidId)
      }
    }


  //
  //  def createInstallation(request: InstallationRequest): Free[F, Installation] = for {
  //    newInstallation <- userServices.createInstallation(toInstallationRequestProcess(request))
  //  } yield fromInstallationProcesses(newInstallation)
  //
  //  def updateInstallation(installationId: String, request: InstallationRequest): Free[F, Installation] = for {
  //    updateInstallation <- userServices.updateInstallation(toInstallationRequestProcess(request), installationId)
  //  } yield fromInstallationProcesses(updateInstallation)

}

object UserProcesses {

  implicit def userProcesses[F[_]](implicit userServices: UserServices[F]) = new UserProcesses()

}
