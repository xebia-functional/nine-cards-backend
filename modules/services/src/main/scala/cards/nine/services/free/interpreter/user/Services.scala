package cards.nine.services.free.interpreter.user

import cards.nine.domain.account._
import cards.nine.services.free.algebra.User._
import cards.nine.services.free.domain.Installation.{ Queries ⇒ InstallationQueries }
import cards.nine.services.free.domain.User.{ Queries ⇒ UserQueries }
import cards.nine.services.free.domain.{ Installation, User }
import cards.nine.services.persistence.Persistence
import cats.~>
import doobie.imports._

class Services(
  userPersistence: Persistence[User],
  installationPersistence: Persistence[Installation]
) extends (Ops ~> ConnectionIO) {

  def addUser[K: Composite](email: Email, apiKey: ApiKey, sessionToken: SessionToken): ConnectionIO[K] =
    userPersistence.updateWithGeneratedKeys[K](
      sql    = UserQueries.insert,
      fields = User.allFields,
      values = (email, sessionToken, apiKey)
    )

  def getUserByEmail(email: Email): ConnectionIO[Option[User]] =
    userPersistence.fetchOption(UserQueries.getByEmail, email)

  def getUserBySessionToken(sessionToken: SessionToken): ConnectionIO[Option[User]] =
    userPersistence.fetchOption(UserQueries.getBySessionToken, sessionToken)

  def createInstallation[K: Composite](
    userId: Long,
    deviceToken: Option[DeviceToken],
    androidId: AndroidId
  ): ConnectionIO[K] =
    userPersistence.updateWithGeneratedKeys[K](
      sql    = InstallationQueries.insert,
      fields = Installation.allFields,
      values = (userId, deviceToken, androidId)
    )

  def getInstallationByUserAndAndroidId(
    userId: Long,
    androidId: AndroidId
  ): ConnectionIO[Option[Installation]] =
    installationPersistence.fetchOption(
      sql    = InstallationQueries.getByUserAndAndroidId,
      values = (userId, androidId)
    )

  def getSubscribedInstallationByCollection(publicIdentifier: String): ConnectionIO[List[Installation]] =
    installationPersistence.fetchList(InstallationQueries.getSubscribedByCollection, publicIdentifier)

  def updateInstallation[K: Composite](userId: Long, deviceToken: Option[DeviceToken], androidId: AndroidId): ConnectionIO[K] =
    userPersistence.updateWithGeneratedKeys[K](
      sql    = InstallationQueries.updateDeviceToken,
      fields = Installation.allFields,
      values = (deviceToken, userId, androidId)
    )

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