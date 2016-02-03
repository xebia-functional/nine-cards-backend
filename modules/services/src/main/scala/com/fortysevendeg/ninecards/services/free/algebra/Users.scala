package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.domain.{Installation, User}

import scala.language.higherKinds

object Users {

  sealed trait UserOps[A]

  case class AddUser(user: User) extends UserOps[User]

  case class GetUserByEmail(email: String) extends UserOps[Option[User]]

  case class CreateInstallation(
    userId: Long,
    androidId: String,
    deviceToken: Option[String]) extends UserOps[Installation]

  case class UpdateInstallation(
    userId: Long,
    androidId: String,
    deviceToken: Option[String]) extends UserOps[Installation]

  class UserServices[F[_]](implicit I: Inject[UserOps, F]) {

    def addUser(user: User): Free[F, User] = Free.inject[UserOps, F](AddUser(user))

    def getUserByEmail(email: String): Free[F, Option[User]] = Free.inject[UserOps, F](GetUserByEmail(email))

    def createInstallation(
      userId: Long,
      androidId: String,
      deviceToken: Option[String]): Free[F, Installation] =
      Free.inject[UserOps, F](CreateInstallation(userId, androidId, deviceToken))

    def updateInstallation(
      userId: Long,
      androidId: String,
      deviceToken: Option[String]): Free[F, Installation] =
      Free.inject[UserOps, F](UpdateInstallation(userId, androidId, deviceToken))

  }

  object UserServices {

    implicit def dataSource[F[_]](implicit inject: Inject[UserOps, F]): UserServices[F] = new UserServices[F]

  }

}