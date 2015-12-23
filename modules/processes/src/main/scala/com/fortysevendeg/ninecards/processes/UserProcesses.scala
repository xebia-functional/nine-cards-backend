package com.fortysevendeg.ninecards.processes


import java.util.UUID

import cats.free.Free
import com.fortysevendeg.ninecards.processes.domain.User
import com.fortysevendeg.ninecards.processes.messages.AddUserRequest
import com.fortysevendeg.ninecards.services.free.algebra.Users.UserServices
import com.fortysevendeg.ninecards.processes.domain.{Installation, User}
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.services.free.domain.{User => UserAppServices, AuthData => AuthDataServices}
import scala.language.higherKinds

class UserProcesses[F[_]](
  implicit userServices: UserServices[F]) {

  def getUserById(userId: String): Free[F, User] = for {
    persistenceApps <- userServices.getUserById(userId)
  } yield (persistenceApps map toUserApp).getOrElse(throw new RuntimeException(""))

  def signUpUser(userRequest: AddUserRequest): Free[F, User] = for {
    maybeUser <- userServices.getUserByEmail(userRequest.authData.google.email)
    user <- createOrReturnUser(maybeUser, userRequest)
  } yield toUserApp(user)

  private def createOrReturnUser(maybeUser: Option[UserAppServices], data: AddUserRequest): Free[F, UserAppServices] =
    maybeUser match {
      case Some(user) => userServices.addUser(user)
      case None => userServices.insertUser(createFromGoogle(data))
    }

  private def createFromGoogle(addUserRequest: AddUserRequest): UserAppServices =
    UserAppServices(
      sessionToken = Option(UUID.randomUUID().toString),
      authData = Option(AuthDataServices(google = Option(toGoogleAuthDataRequestProcess(addUserRequest.authData.google)))))

  def createInstallation(request: InstallationRequest): Free[F, Installation] = for {
    newInstallation <- userSevices.createInstallation(toInstallationRequestProcess(request))
  } yield fromInstallationProcesses(newInstallation)
  
}

object UserProcesses {

  implicit def userProcesses[F[_]](implicit userSevices: UserServices[F]) = new UserProcesses()

}
