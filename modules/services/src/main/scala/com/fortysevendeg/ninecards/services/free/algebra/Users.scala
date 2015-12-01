package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.domain.User

import scala.language.higherKinds

object Users {

  sealed trait UserOps[A]

  case class AddUser(user: User) extends UserOps[User]

  case class GetUserByUserName(username: String) extends UserOps[Option[User]]

  case class CheckPassword(password: String) extends UserOps[Boolean]

  class UserServices[F[_]](implicit I: Inject[UserOps, F]) {

    def addUser(user: User): Free[F, User] = Free.inject[UserOps, F](AddUser(user))

    def getUserByUserName(username: String): Free[F, Option[User]] = Free.inject[UserOps, F](GetUserByUserName(username))

    def checkPassword(password: String): Free[F, Boolean] = Free.inject[UserOps, F](CheckPassword(password))
  }

  object UserServices {

    implicit def dataSource[F[_]](
      implicit I: Inject[UserOps, F]): UserServices[F] =
      new UserServices[F]

  }

}