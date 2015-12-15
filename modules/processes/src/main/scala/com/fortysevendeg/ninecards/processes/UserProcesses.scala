package com.fortysevendeg.ninecards.processes

import com.fortysevendeg.ninecards.processes.messages.AddUserRequest
import com.fortysevendeg.ninecards.services.free.algebra.Users.UserServices
import com.fortysevendeg.ninecards.services.free.domain.User

import scala.language.higherKinds
import scalaz.Free

class UserProcesses[F[_]](
  implicit userServices: UserServices[F]) {

  def addUser(user: AddUserRequest): Free[F, User] = for {
    persistenceApps <- userServices.addUser(user)
  } yield (persistenceApps map toUserApp).getOrElse(throw new RuntimeException(""))

}
