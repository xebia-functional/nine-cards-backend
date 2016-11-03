package cards.nine.services.free.interpreter.user

import cards.nine.commons.NineCardsErrors.{ InstallationNotFound, UserNotFound }
import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.account._
import cards.nine.services.free.algebra.User._
import cards.nine.services.free.domain.Installation.{ Queries ⇒ InstallationQueries }
import cards.nine.services.free.domain.User.{ Queries ⇒ UserQueries }
import cards.nine.services.free.domain.{ Installation, User }
import cards.nine.services.persistence.Persistence
import cats.syntax.either._
import cats.~>
import doobie.imports._

class Services(
  userPersistence: Persistence[User],
  installationPersistence: Persistence[Installation]
) extends (Ops ~> ConnectionIO) {

  def addUser[K: Composite](email: Email, apiKey: ApiKey, sessionToken: SessionToken): ConnectionIO[Result[K]] =
    userPersistence.updateWithGeneratedKeys[K](
      sql    = UserQueries.insert,
      fields = User.allFields,
      values = (email, sessionToken, apiKey)
    ) map Either.right

  def getUserByEmail(email: Email): ConnectionIO[Result[User]] =
    userPersistence.fetchOption(UserQueries.getByEmail, email) map {
      Either.fromOption(_, UserNotFound(s"User with email ${email.value} not found"))
    }

  def getUserBySessionToken(sessionToken: SessionToken): ConnectionIO[Result[User]] =
    userPersistence.fetchOption(UserQueries.getBySessionToken, sessionToken) map {
      Either.fromOption(_, UserNotFound(s"User with sessionToken ${sessionToken.value} not found"))
    }

  def createInstallation[K: Composite](
    userId: Long,
    deviceToken: Option[DeviceToken],
    androidId: AndroidId
  ): ConnectionIO[Result[K]] =
    userPersistence.updateWithGeneratedKeys[K](
      sql    = InstallationQueries.insert,
      fields = Installation.allFields,
      values = (userId, deviceToken, androidId)
    ) map Either.right

  def getInstallationByUserAndAndroidId(
    userId: Long,
    androidId: AndroidId
  ): ConnectionIO[Result[Installation]] =
    installationPersistence.fetchOption(
      sql    = InstallationQueries.getByUserAndAndroidId,
      values = (userId, androidId)
    ) map {
      Either.fromOption(_, InstallationNotFound(s"Installation for android id ${androidId.value} not found"))
    }

  def getSubscribedInstallationByCollection(publicIdentifier: String): ConnectionIO[Result[List[Installation]]] =
    installationPersistence.fetchList(
      sql    = InstallationQueries.getSubscribedByCollection,
      values = publicIdentifier
    ) map Either.right

  def updateInstallation[K: Composite](userId: Long, deviceToken: Option[DeviceToken], androidId: AndroidId): ConnectionIO[Result[K]] =
    userPersistence.updateWithGeneratedKeys[K](
      sql    = InstallationQueries.updateDeviceToken,
      fields = Installation.allFields,
      values = (deviceToken, userId, androidId)
    ) map Either.right

  def apply[A](fa: Ops[A]): ConnectionIO[A] = fa match {
    case Add(email, apiKey, sessionToken) ⇒
      addUser[User](email, apiKey, sessionToken)
    case AddInstallation(user, deviceToken, androidId) ⇒
      createInstallation[Installation](user, deviceToken, androidId)
    case GetByEmail(email) ⇒
      getUserByEmail(email)
    case GetBySessionToken(sessionToken) ⇒
      getUserBySessionToken(sessionToken)
    case GetInstallationByUserAndAndroidId(user, androidId) ⇒
      getInstallationByUserAndAndroidId(user, androidId)
    case GetSubscribedInstallationByCollection(collectionPublicId) ⇒
      getSubscribedInstallationByCollection(collectionPublicId)
    case UpdateInstallation(user, deviceToken, androidId) ⇒
      updateInstallation[Installation](user, deviceToken, androidId)
  }
}

object Services {

  case class UserData(
    email: String,
    apiKey: String,
    sessionToken: String
  )

  def services(
    implicit
    userPersistence: Persistence[User],
    installationPersistence: Persistence[Installation]
  ) =
    new Services(userPersistence, installationPersistence)
}