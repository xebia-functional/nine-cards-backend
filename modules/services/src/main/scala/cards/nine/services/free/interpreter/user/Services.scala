/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.services.free.interpreter.user

import cards.nine.commons.NineCardsErrors.{ InstallationNotFound, UserNotFound }
import cards.nine.domain.account._
import cards.nine.services.common.PersistenceService
import cards.nine.services.common.PersistenceService._
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

  def addUser(email: Email, apiKey: ApiKey, sessionToken: SessionToken): PersistenceService[User] =
    PersistenceService {
      userPersistence.updateWithGeneratedKeys(
        sql    = UserQueries.insert,
        fields = User.allFields,
        values = (email, sessionToken, apiKey)
      )
    }

  def getUserByEmail(email: Email): PersistenceService[User] =
    userPersistence.fetchOption(UserQueries.getByEmail, email) map {
      Either.fromOption(_, UserNotFound(s"User with email ${email.value} not found"))
    }

  def getUserBySessionToken(sessionToken: SessionToken): PersistenceService[User] =
    userPersistence.fetchOption(UserQueries.getBySessionToken, sessionToken) map {
      Either.fromOption(_, UserNotFound(s"User with sessionToken ${sessionToken.value} not found"))
    }

  def createInstallation(
    userId: Long,
    deviceToken: Option[DeviceToken],
    androidId: AndroidId
  ): PersistenceService[Installation] =
    PersistenceService {
      installationPersistence.updateWithGeneratedKeys(
        sql    = InstallationQueries.insert,
        fields = Installation.allFields,
        values = (userId, deviceToken, androidId)
      )
    }

  def getInstallationByUserAndAndroidId(
    userId: Long,
    androidId: AndroidId
  ): PersistenceService[Installation] =
    installationPersistence.fetchOption(
      sql    = InstallationQueries.getByUserAndAndroidId,
      values = (userId, androidId)
    ) map {
      Either.fromOption(_, InstallationNotFound(s"Installation for android id ${androidId.value} not found"))
    }

  def getSubscribedInstallationByCollection(publicIdentifier: String): PersistenceService[List[Installation]] =
    PersistenceService {
      installationPersistence.fetchList(
        sql    = InstallationQueries.getSubscribedByCollection,
        values = publicIdentifier
      )
    }

  def updateInstallation(userId: Long, deviceToken: Option[DeviceToken], androidId: AndroidId): PersistenceService[Installation] =
    PersistenceService {
      installationPersistence.updateWithGeneratedKeys(
        sql    = InstallationQueries.updateDeviceToken,
        fields = Installation.allFields,
        values = (deviceToken, userId, androidId)
      )
    }

  def apply[A](fa: Ops[A]): ConnectionIO[A] = fa match {
    case Add(email, apiKey, sessionToken) ⇒
      addUser(email, apiKey, sessionToken)
    case AddInstallation(user, deviceToken, androidId) ⇒
      createInstallation(user, deviceToken, androidId)
    case GetByEmail(email) ⇒
      getUserByEmail(email)
    case GetBySessionToken(sessionToken) ⇒
      getUserBySessionToken(sessionToken)
    case GetInstallationByUserAndAndroidId(user, androidId) ⇒
      getInstallationByUserAndAndroidId(user, androidId)
    case GetSubscribedInstallationByCollection(collectionPublicId) ⇒
      getSubscribedInstallationByCollection(collectionPublicId)
    case UpdateInstallation(user, deviceToken, androidId) ⇒
      updateInstallation(user, deviceToken, androidId)
  }
}

object Services {

  case class UserData(
    email: String,
    sessionToken: String,
    apiKey: String
  )

  def services(
    implicit
    userPersistence: Persistence[User],
    installationPersistence: Persistence[Installation]
  ) =
    new Services(userPersistence, installationPersistence)
}