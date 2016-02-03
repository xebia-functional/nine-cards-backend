package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.domain.{Installation, User}
import scala.language.higherKinds

object Users {

  sealed trait UserOps[A]

  case class GetUserByEmail(email: String) extends UserOps[Option[User]]

  case class CreateUser(email: String, androidId: String, sessionToken: String) extends UserOps[User]

  case class GetUser(user: User) extends UserOps[User]

  case class GetInstallation(installation: Installation) extends UserOps[Installation]

  case class GetInstallationByAndroidId(androidId: String) extends UserOps[Option[Installation]]

  case class CreateInstallation(userId: Long, androidId: String) extends UserOps[Installation]

  class UserServices[F[_]](implicit I: Inject[UserOps, F]) {

    def getUserByEmail(email: String): Free[F, Option[User]] = Free.inject[UserOps, F](GetUserByEmail(email))

    def createUser(email: String, androidId: String, sessionToken: String): Free[F, User] = Free.inject[UserOps, F](CreateUser(email, androidId, sessionToken))

    def getUser(user: User): Free[F, User] = Free.inject[UserOps, F](GetUser(user))

    def getInstallation(installation: Installation): Free[F, Installation] = Free.inject[UserOps, F](GetInstallation(installation))

    def getInstallationByAndroidId(androidId: String): Free[F, Option[Installation]] = Free.inject[UserOps, F](GetInstallationByAndroidId(androidId))

    def createInstallation(userId: Long, androidId: String): Free[F, Installation] = Free.inject[UserOps, F](CreateInstallation(userId, androidId))

  }

  object UserServices {

    implicit def dataSource[F[_]](implicit inject: Inject[UserOps, F]): UserServices[F] = new UserServices[F]

  }

}