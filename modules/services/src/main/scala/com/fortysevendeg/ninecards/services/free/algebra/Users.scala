package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.domain.{Device, Installation, User}
import scala.language.higherKinds

object Users {

  sealed trait UserOps[A]

  case class GetUserByEmail(email: String) extends UserOps[Option[User]]

  case class CreateUser(email: String, androidId: String, sessionToken: String) extends UserOps[User]

  case class GetUser(user: User) extends UserOps[User]

  case class GetDeviceByAndroidId(androidId: String) extends UserOps[Option[Device]]

  case class CreateDevice(userId: Long, androidId: String) extends UserOps[Device]

  case class GetDevice(device: Device) extends UserOps[Device]

  case class CreateInstallation(installation: Installation) extends UserOps[Installation]

  case class UpdateInstallation(installation: Installation, installationId: String) extends UserOps[Installation]

  class UserServices[F[_]](implicit I: Inject[UserOps, F]) {

    def getUserByEmail(email: String): Free[F, Option[User]] = Free.inject[UserOps, F](GetUserByEmail(email))

    def createUser(email: String, androidId: String, sessionToken: String): Free[F, User] = Free.inject[UserOps, F](CreateUser(email, androidId, sessionToken))

    def getUser(user: User): Free[F, User] = Free.inject[UserOps, F](GetUser(user))

    def getDeviceByAndroidId(androidId: String): Free[F, Option[Device]] = Free.inject[UserOps, F](GetDeviceByAndroidId(androidId))

    def createDevice(userId: Long, androidId: String): Free[F, Device] = Free.inject[UserOps, F](CreateDevice(userId, androidId))

    def getDevice(device: Device): Free[F, Device] = Free.inject[UserOps, F](GetDevice(device))

    def createInstallation(installation: Installation): Free[F, Installation] = Free.inject[UserOps, F](CreateInstallation(installation))

    def updateInstallation(installation: Installation, installationId: String): Free[F, Installation] = Free.inject[UserOps, F](UpdateInstallation(installation, installationId))

  }

  object UserServices {

    implicit def dataSource[F[_]](implicit inject: Inject[UserOps, F]): UserServices[F] = new UserServices[F]

  }

}