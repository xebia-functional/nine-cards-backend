package com.fortysevendeg.ninecards.processes


import java.util.UUID

import cats.free.Free
import com.fortysevendeg.ninecards.processes.domain.{AuthData, User}
import com.fortysevendeg.ninecards.processes.messages.AddUserRequest
import com.fortysevendeg.ninecards.services.free.algebra.Users.UserServices
import com.fortysevendeg.ninecards.processes.converters.Converters._
import scala.language.higherKinds

class UserProcesses[F[_]](
  implicit userServices: UserServices[F]) {

  def getUserById(userId: String): Free[F, User] = for {
    persistenceApps <- userServices.getUserById(userId)
  } yield (persistenceApps map toUserApp).getOrElse(throw new RuntimeException(""))

  def signUpUser(userRequest: AddUserRequest): Free[F, User] = for {
    maybeUser <- userServices.getUserByEmail(userRequest.authData.google.email)
    user <- createOrReturnUser(maybeUser map toUserApp, userRequest)
  } yield user

  def createOrReturnUser(maybeUser: Option[User], data: AddUserRequest): Free[F, User] =
    maybeUser match {
      case Some(user) => userServices.addUser(fromUserApp(user)) map toUserApp
      case None => userServices.insertUser(fromUserApp(createFromGoogle(data))) map toUserApp
    }

  def createFromGoogle(addUserRequest: AddUserRequest): User =
    User(
      sessionToken = Option(UUID.randomUUID().toString),
      authData = Option(AuthData(google = Option(toGoogleAuthDataRequestProcess(addUserRequest.authData.google)))))

}

object UserProcesses {

  implicit def userProcesses[F[_]](implicit userServices: UserServices[F]) = new UserProcesses()

}

