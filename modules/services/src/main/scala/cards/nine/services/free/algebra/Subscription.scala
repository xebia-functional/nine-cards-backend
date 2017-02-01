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
import cards.nine.commons.NineCardsService.{ NineCardsService, Result }
import cards.nine.services.free.domain.SharedCollectionSubscription
import cats.free.:<:

object Subscription {

  sealed trait Ops[A]

  case class Add(collection: Long, user: Long, collectionPublicId: String) extends Ops[Result[Int]]

  case class GetByCollectionAndUser(collection: Long, user: Long) extends Ops[Result[Option[SharedCollectionSubscription]]]

  case class GetByCollection(collection: Long) extends Ops[Result[List[SharedCollectionSubscription]]]

  case class GetByUser(user: Long) extends Ops[Result[List[SharedCollectionSubscription]]]

  case class RemoveByCollectionAndUser(collection: Long, user: Long) extends Ops[Result[Int]]

  class Services[F[_]](implicit I: Ops :<: F) {

    def add(collection: Long, user: Long, collectionPublicId: String): NineCardsService[F, Int] =
      NineCardsService(Add(collection, user, collectionPublicId))

    def getByCollectionAndUser(collection: Long, user: Long): NineCardsService[F, Option[SharedCollectionSubscription]] =
      NineCardsService(GetByCollectionAndUser(collection, user))

    def getByCollection(collection: Long): NineCardsService[F, List[SharedCollectionSubscription]] =
      NineCardsService(GetByCollection(collection))

    def getByUser(user: Long): NineCardsService[F, List[SharedCollectionSubscription]] =
      NineCardsService(GetByUser(user))

    def removeByCollectionAndUser(collection: Long, user: Long): NineCardsService[F, Int] =
      NineCardsService(RemoveByCollectionAndUser(collection, user))
  }

  object Services {

    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] = new Services

  }

}
