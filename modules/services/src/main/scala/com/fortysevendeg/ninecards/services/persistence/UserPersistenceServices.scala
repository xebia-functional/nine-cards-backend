package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.Installation.{Queries => InstallationQueries}
import com.fortysevendeg.ninecards.services.free.domain.User.{Queries => UserQueries}
import com.fortysevendeg.ninecards.services.free.domain._
import doobie.imports._

class UserPersistenceServices(
  implicit userPersistence: Persistence[User],
  installationPersistence: Persistence[Installation]) {

  def addUser[K](
    email: String,
    apiKey: String,
    sessionToken: String)(implicit ev: Composite[K]): ConnectionIO[K] =
    userPersistence.updateWithGeneratedKeys[K](
      sql = UserQueries.insert,
      fields = User.allFields,
      values = (email, sessionToken, apiKey))

  def getUserByEmail(
    email: String): ConnectionIO[Option[User]] =
    userPersistence.fetchOption(UserQueries.getByEmail, email)

  def getUserBySessionToken(
    sessionToken: String): ConnectionIO[Option[User]] =
    userPersistence.fetchOption(UserQueries.getBySessionToken, sessionToken)

  def createInstallation[K](
    userId: Long,
    deviceToken: Option[String],
    androidId: String)(implicit ev: Composite[K]): ConnectionIO[K] =
    userPersistence.updateWithGeneratedKeys[K](
      sql = InstallationQueries.insert,
      fields = Installation.allFields,
      values = (userId, deviceToken, androidId))

  def getInstallationByUserAndAndroidId(
    userId: Long,
    androidId: String): ConnectionIO[Option[Installation]] =
    installationPersistence.fetchOption(
      sql = InstallationQueries.getByUserAndAndroidId,
      values = (userId, androidId))

  def getInstallationById(
    id: Long): ConnectionIO[Option[Installation]] =
    installationPersistence.fetchOption(InstallationQueries.getById, id)

  def updateInstallation[K](
    userId: Long,
    deviceToken: Option[String],
    androidId: String)(implicit ev: Composite[K]): ConnectionIO[K] =
    userPersistence.updateWithGeneratedKeys[K](
      sql = InstallationQueries.updateDeviceToken,
      fields = Installation.allFields,
      values = (deviceToken, userId, androidId))

}

object UserPersistenceServices {

  case class UserData(
    email: String,
    apiKey: String,
    sessionToken: String)

  implicit def userPersistenceServices(
    implicit userPersistence: Persistence[User],
    installationPersistence: Persistence[Installation]) = new UserPersistenceServices
}
