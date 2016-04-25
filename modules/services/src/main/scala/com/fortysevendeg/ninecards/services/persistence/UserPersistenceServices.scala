package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.Installation.{ Queries ⇒ InstallationQueries }
import com.fortysevendeg.ninecards.services.free.domain.User.{ Queries ⇒ UserQueries }
import com.fortysevendeg.ninecards.services.free.domain._
import doobie.imports._

class UserPersistenceServices(
  implicit
  userPersistence: Persistence[User],
  installationPersistence: Persistence[Installation]
) {

  type CIO[K] = ConnectionIO[K]

  def addUser[K: Composite](email: String, apiKey: String, sessionToken: String): CIO[K] =
    userPersistence.updateWithGeneratedKeys[K](
      sql    = UserQueries.insert,
      fields = User.allFields,
      values = (email, sessionToken, apiKey)
    )

  def getUserByEmail(email: String): CIO[Option[User]] =
    userPersistence.fetchOption(UserQueries.getByEmail, email)

  def getUserBySessionToken(sessionToken: String): CIO[Option[User]] =
    userPersistence.fetchOption(UserQueries.getBySessionToken, sessionToken)

  def createInstallation[K: Composite](userId: Long, deviceToken: Option[String], androidId: String): CIO[K] =
    userPersistence.updateWithGeneratedKeys[K](
      sql    = InstallationQueries.insert,
      fields = Installation.allFields,
      values = (userId, deviceToken, androidId)
    )

  def getInstallationByUserAndAndroidId(userId: Long, androidId: String): CIO[Option[Installation]] =
    installationPersistence.fetchOption(
      sql    = InstallationQueries.getByUserAndAndroidId,
      values = (userId, androidId)
    )

  def getInstallationById(id: Long): CIO[Option[Installation]] =
    installationPersistence.fetchOption(InstallationQueries.getById, id)

  def updateInstallation[K: Composite](userId: Long, deviceToken: Option[String], androidId: String): CIO[K] =
    userPersistence.updateWithGeneratedKeys[K](
      sql    = InstallationQueries.updateDeviceToken,
      fields = Installation.allFields,
      values = (deviceToken, userId, androidId)
    )

}

object UserPersistenceServices {

  case class UserData(
    email: String,
    apiKey: String,
    sessionToken: String
  )

  implicit def userPersistenceServices(
    implicit
    userPersistence: Persistence[User],
    installationPersistence: Persistence[Installation]
  ) = new UserPersistenceServices
}
