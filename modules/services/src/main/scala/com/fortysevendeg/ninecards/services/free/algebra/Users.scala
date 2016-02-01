package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.domain.{Device, User}

import scala.language.higherKinds

object Users {

  sealed trait UserOps[A]

  case class AddUser(user: User) extends UserOps[User]

  case class GetUserByEmail(email: String) extends UserOps[Option[User]]

  case class CreateDevice(
    userId: Long,
    androidId: String,
    deviceToken: Option[String]) extends UserOps[Device]

  case class UpdateDevice(
    userId: Long,
    androidId: String,
    deviceToken: Option[String]) extends UserOps[Device]

  class UserServices[F[_]](implicit I: Inject[UserOps, F]) {

    def addUser(user: User): Free[F, User] = Free.inject[UserOps, F](AddUser(user))

    def getUserByEmail(email: String): Free[F, Option[User]] = Free.inject[UserOps, F](GetUserByEmail(email))

    def createDevice(
      userId: Long,
      androidId: String,
      deviceToken: Option[String]): Free[F, Device] =
      Free.inject[UserOps, F](CreateDevice(userId, androidId, deviceToken))

    def updateDevice(
      userId: Long,
      androidId: String,
      deviceToken: Option[String]): Free[F, Device] =
      Free.inject[UserOps, F](UpdateDevice(userId, androidId, deviceToken))

  }

  object UserServices {

    implicit def dataSource[F[_]](implicit inject: Inject[UserOps, F]): UserServices[F] = new UserServices[F]

  }

}