package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.domain.{Installation, User}
import scala.language.higherKinds

object Users {

  sealed trait UserOps[A]

  case class AddUser(user: User) extends UserOps[User]

  case class GetUserByEmail(email: String) extends UserOps[Option[User]]

  case class CreateInstallation(installation: Installation) extends UserOps[Installation]

  case class UpdateInstallation(installation: Installation, installationId: String) extends UserOps[Installation]

  class UserServices[F[_]](implicit I: Inject[UserOps, F]) {

    def addUser(user: User): Free[F, User] = Free.inject[UserOps, F](AddUser(user))

    def getUserByEmail(email: String): Free[F, Option[User]] = Free.inject[UserOps, F](GetUserByEmail(email))

    def createInstallation(installation: Installation): Free[F, Installation] = Free.inject[UserOps, F](CreateInstallation(installation))

    def updateInstallation(installation: Installation, installationId: String): Free[F, Installation] = Free.inject[UserOps, F](UpdateInstallation(installation, installationId))

  }

  object UserServices {

    implicit def dataSource[F[_]](implicit inject: Inject[UserOps, F]): UserServices[F] = new UserServices[F]

  }

}