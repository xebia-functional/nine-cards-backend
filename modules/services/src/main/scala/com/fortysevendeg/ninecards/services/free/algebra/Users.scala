package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.domain.{Installation, User}
import scala.language.higherKinds

object Users {

  sealed trait UserOps[A]

  case class GetUserByEmail(email: String) extends UserOps[Option[User]]

  case class CreateUser(email: String, androidId: String, sessionToken: String) extends UserOps[User]

  case class GetDeviceByAndroidId(androidId: String) extends UserOps[Option[Installation]]

  case class CreateDevice(userId: Long, androidId: String) extends UserOps[Installation]

  case class CreateInstallation(installation: Installation) extends UserOps[Installation]

  case class UpdateInstallation(installation: Installation, installationId: String) extends UserOps[Installation]

  class UserServices[F[_]](implicit I: Inject[UserOps, F]) {

    def getUserByEmail(email: String): Free[F, Option[User]] = Free.inject[UserOps, F](GetUserByEmail(email))

    def createUser(email: String, androidId: String, sessionToken: String): Free[F, User] = Free.inject[UserOps, F](CreateUser(email, androidId, sessionToken))

    def getDeviceByAndroidId(androidId: String): Free[F, Option[Installation]] = Free.inject[UserOps, F](GetDeviceByAndroidId(androidId))

    def createInstallation(userId: Long, androidId: String): Free[F, Installation] = Free.inject[UserOps, F](CreateDevice(userId, androidId))

  }

  object UserServices {

    implicit def dataSource[F[_]](implicit inject: Inject[UserOps, F]): UserServices[F] = new UserServices[F]

  }

}