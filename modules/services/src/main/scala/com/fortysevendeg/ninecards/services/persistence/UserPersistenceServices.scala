package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.Installation.{Queries => InstallationQueries}
import com.fortysevendeg.ninecards.services.free.domain.User.{Queries => UserQueries}
import com.fortysevendeg.ninecards.services.free.domain._
import doobie.imports.ConnectionIO

class UserPersistenceServices(implicit persistence: PersistenceImpl) {

  def addUser(
    email: String,
    sessionToken: String) =
    persistence.updateWithGeneratedKeys[(String, String), User](UserQueries.insert, User.allFields, (email, sessionToken))

  def getUserByEmail(
    email: String): ConnectionIO[Option[User]] =
    persistence.fetchOption[String, User](UserQueries.getByEmail, email)

  def getUserBySessionToken(
    sessionToken: String): ConnectionIO[Option[User]] =
    persistence.fetchOption[String, User](UserQueries.getBySessionToken, sessionToken)

  def createInstallation(
    userId: Long,
    deviceToken: Option[String],
    androidId: String): ConnectionIO[Installation] =
    persistence.updateWithGeneratedKeys[(Long, Option[String], String), Installation](InstallationQueries.insert, Installation.allFields, (userId, deviceToken, androidId))

  def getInstallationByUserAndAndroidId(
    userId: Long,
    androidId: String): ConnectionIO[Option[Installation]] =
    persistence.fetchOption[(Long, String), Installation](InstallationQueries.getByUserAndAndroidId, (userId, androidId))

  def updateInstallation(
    userId: Long,
    deviceToken: Option[String],
    androidId: String): ConnectionIO[Installation] =
    persistence.updateWithGeneratedKeys[(Option[String], Long, String), Installation](InstallationQueries.updateDeviceToken, Installation.allFields, (deviceToken, userId, androidId))

}

object UserPersistenceServices {

  implicit def userPersistenceImpl(implicit persistence: PersistenceImpl) = new UserPersistenceServices()
}
