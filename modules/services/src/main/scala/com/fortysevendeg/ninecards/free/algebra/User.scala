package com.fortysevendeg.ninecards.free.algebra

import com.fortysevendeg.ninecards.free.domain.User

import scalaz.Free._
import scalaz.{Free, Inject}

object user {

  def lift[F[_], G[_], A](fa: F[A])(implicit I: Inject[F, G]): FreeC[G, A] = Free.liftFC(I.inj(fa))

  sealed trait UserOps[A]

  case class AddUser(user: User) extends UserOps[User]

  case class GetUserByUserName(username: String) extends UserOps[Option[User]]

  case class CheckPassword(password: String) extends UserOps[Boolean]

  class UserServices[F[_]](implicit I: Inject[UserOps, F]) {

    def addUser(user: User): Free.FreeC[F, User] = lift[UserOps, F, User](AddUser(user))

    def getUserByUserName(username: String): Free.FreeC[F, Option[User]] = lift[UserOps, F, Option[User]](GetUserByUserName(username))

    def checkPassword(password: String): Free.FreeC[F, Boolean] = lift[UserOps, F, Boolean](CheckPassword(password))
  }

  object UserServices {

    implicit def dataSource[F[_]](implicit I: Inject[UserOps, F]): UserServices[F] = new UserServices[F]

  }
}