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
package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.account._
import cards.nine.services.free.domain
import cats.free.Inject

object User {

  sealed trait Ops[A]

  case class Add(email: Email, apiKey: ApiKey, sessionToken: SessionToken) extends Ops[Result[domain.User]]

  case class AddInstallation(user: Long, deviceToken: Option[DeviceToken], androidId: AndroidId) extends Ops[Result[domain.Installation]]

  case class GetByEmail(email: Email) extends Ops[Result[domain.User]]

  case class GetBySessionToken(sessionToken: SessionToken) extends Ops[Result[domain.User]]

  case class GetInstallationByUserAndAndroidId(user: Long, androidId: AndroidId) extends Ops[Result[domain.Installation]]

  case class GetSubscribedInstallationByCollection(collectionPublicId: String) extends Ops[Result[List[domain.Installation]]]

  case class UpdateInstallation(user: Long, deviceToken: Option[DeviceToken], androidId: AndroidId) extends Ops[Result[domain.Installation]]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def add(email: Email, apiKey: ApiKey, sessionToken: SessionToken): NineCardsService[F, domain.User] =
      NineCardsService(Add(email, apiKey, sessionToken))

    def addInstallation(user: Long, deviceToken: Option[DeviceToken], androidId: AndroidId): NineCardsService[F, domain.Installation] =
      NineCardsService(AddInstallation(user, deviceToken, androidId))

    def getByEmail(email: Email): NineCardsService[F, domain.User] =
      NineCardsService(GetByEmail(email))

    def getBySessionToken(sessionToken: SessionToken): NineCardsService[F, domain.User] =
      NineCardsService(GetBySessionToken(sessionToken))

    def getInstallationByUserAndAndroidId(user: Long, androidId: AndroidId): NineCardsService[F, domain.Installation] =
      NineCardsService(GetInstallationByUserAndAndroidId(user, androidId))

    def getSubscribedInstallationByCollection(collectionPublicId: String): NineCardsService[F, List[domain.Installation]] =
      NineCardsService(GetSubscribedInstallationByCollection(collectionPublicId))

    def updateInstallation(user: Long, deviceToken: Option[DeviceToken], androidId: AndroidId): NineCardsService[F, domain.Installation] =
      NineCardsService(UpdateInstallation(user, deviceToken, androidId))
  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] = new Services

  }

}
