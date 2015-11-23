package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.algebra.Utils._
import com.fortysevendeg.ninecards.services.free.domain.User

import scala.language.higherKinds

object Users {

  sealed trait UserOps[A]

  case class AddUser(user: User) extends UserOps[User]

  case class GetUserByUserName(username: String) extends UserOps[Option[User]]

  case class CheckPassword(password: String) extends UserOps[Boolean]

  class UserServices[F[_]](implicit I: Inject[UserOps, F]) {

    def addUser(user: User): Free[F, User] = lift[UserOps, F, User](AddUser(user))

    def getUserByUserName(username: String): Free[F, Option[User]] = lift[UserOps, F, Option[User]](GetUserByUserName(username))

    def checkPassword(password: String): Free[F, Boolean] = lift[UserOps, F, Boolean](CheckPassword(password))
  }

  object UserServices {

    implicit def dataSource[F[_]](implicit I: Inject[UserOps, F]): UserServices[F] = new UserServices[F]

  }

}