package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.Installation.{Queries => InstallationQueries}
import com.fortysevendeg.ninecards.services.free.domain.User.{Queries => UserQueries}
import com.fortysevendeg.ninecards.services.free.domain._
import doobie.imports._

class UserPersistenceServices(implicit persistence: PersistenceImpl) {

  def addUser[K](
    email: String,
    sessionToken: String)(implicit ev: Composite[K]): ConnectionIO[K] =
    persistence.updateWithGeneratedKeys[(String, String), K](UserQueries.insert, User.allFields, (email, sessionToken))

  def getUserByEmail(
    email: String): ConnectionIO[Option[User]] =
    persistence.fetchOption[String, User](UserQueries.getByEmail, email)

  def createInstallation[K](
    userId: Long,
    deviceToken: Option[String],
    androidId: String)(implicit ev: Composite[K]): ConnectionIO[K] =
    persistence.updateWithGeneratedKeys[(Long, Option[String], String), K](InstallationQueries.insert, Installation.allFields, (userId, deviceToken, androidId))

  def getInstallationByUserAndAndroidId(
    userId: Long,
    androidId: String): ConnectionIO[Option[Installation]] =
    persistence.fetchOption[(Long, String), Installation](InstallationQueries.getByUserAndAndroidId, (userId, androidId))

  def getInstallationById(
    id: Long): ConnectionIO[Option[Installation]] =
    persistence.fetchOption[(Long), Installation](InstallationQueries.getById, id)

  def updateInstallation[K](
    userId: Long,
    deviceToken: Option[String],
    androidId: String)(implicit ev: Composite[K]): ConnectionIO[K] =
    persistence.updateWithGeneratedKeys[(Option[String], Long, String),K] (InstallationQueries.updateDeviceToken, Installation.allFields, (deviceToken, userId, androidId))

}

object UserPersistenceServices {

  implicit def userPersistenceImpl(implicit persistence: PersistenceImpl) = new UserPersistenceServices()
}
