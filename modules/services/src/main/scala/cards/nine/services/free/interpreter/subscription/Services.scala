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
package cards.nine.services.free.interpreter.subscription

import cards.nine.services.common.PersistenceService
import cards.nine.services.common.PersistenceService._
import cards.nine.services.free.algebra.SubscriptionR._
import cards.nine.services.free.domain.SharedCollectionSubscription
import cards.nine.services.free.domain.SharedCollectionSubscription.Queries
import cards.nine.services.persistence.Persistence
import doobie.imports.ConnectionIO

class Services(persistence: Persistence[SharedCollectionSubscription]) extends Handler[ConnectionIO] {

  def add(collectionId: Long, userId: Long, collectionPublicId: String): PersistenceService[Int] =
    PersistenceService {
      persistence.update(
        sql    = Queries.insert,
        values = (collectionId, userId, collectionPublicId)
      )
    }

  def getByCollection(collectionId: Long): PersistenceService[List[SharedCollectionSubscription]] =
    PersistenceService {
      persistence.fetchList(
        sql    = Queries.getByCollection,
        values = collectionId
      )
    }

  def getByCollectionAndUser(collectionId: Long, userId: Long): PersistenceService[Option[SharedCollectionSubscription]] =
    PersistenceService {
      persistence.fetchOption(
        sql    = Queries.getByCollectionAndUser,
        values = (collectionId, userId)
      )
    }

  def getByUser(userId: Long): PersistenceService[List[SharedCollectionSubscription]] =
    PersistenceService {
      persistence.fetchList(
        sql    = Queries.getByUser,
        values = userId
      )
    }

  def removeByCollectionAndUser(collectionId: Long, userId: Long): PersistenceService[Int] =
    PersistenceService {
      persistence.update(
        sql    = Queries.deleteByCollectionAndUser,
        values = (collectionId, userId)
      )
    }

}

object Services {

  def services(implicit persistence: Persistence[SharedCollectionSubscription]) =
    new Services(persistence)
}
