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
package cards.nine.services.free.interpreter.collection

import java.sql.Timestamp

import cards.nine.commons.catscalaz.ScalazInstances
import cards.nine.commons.NineCardsErrors.SharedCollectionNotFound
import cards.nine.domain.application.Package
import cards.nine.domain.pagination.Page
import cards.nine.services.common.PersistenceService
import cards.nine.services.common.PersistenceService._
import cards.nine.services.free.algebra.Collection._
import cards.nine.services.free.domain.SharedCollection.Queries
import cards.nine.services.free.domain._
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import cards.nine.services.persistence.Persistence
import cats.syntax.either._
import cats.Monad
import doobie.contrib.postgresql.pgtypes._
import doobie.imports.ConnectionIO
import shapeless.syntax.std.product._

class Services(
  collectionPersistence: Persistence[SharedCollection]
)(implicit connectionIOMonad: Monad[ConnectionIO]) extends Handler[ConnectionIO] {

  override def add(data: SharedCollectionData): PersistenceService[SharedCollection] =
    PersistenceService {
      collectionPersistence.updateWithGeneratedKeys(
        sql    = Queries.insert,
        fields = SharedCollection.allFields,
        values = data.toTuple
      )
    }

  override def getById(id: Long): PersistenceService[SharedCollection] =
    collectionPersistence.fetchOption(Queries.getById, id) map (
      Either.fromOption(_, SharedCollectionNotFound("Shared collection not found"))
    )

  override def getByPublicId(publicIdentifier: String): PersistenceService[SharedCollection] =
    collectionPersistence.fetchOption(
      sql    = Queries.getByPublicIdentifier,
      values = publicIdentifier
    ) map (
      Either.fromOption(_, SharedCollectionNotFound(s"Shared collection with public identifier $publicIdentifier doesn't exist"))
    )

  override def getByUser(user: Long): PersistenceService[List[SharedCollectionWithAggregatedInfo]] =
    PersistenceService {
      collectionPersistence.fetchListAs[SharedCollectionWithAggregatedInfo](
        sql    = Queries.getByUser,
        values = user
      )
    }

  override def getLatestByCategory(category: String, pageParams: Page): PersistenceService[List[SharedCollection]] =
    PersistenceService {
      collectionPersistence.fetchList(
        sql    = Queries.getLatestByCategory,
        values = (category, pageParams.pageSize, pageParams.pageNumber)
      )
    }

  override def getTopByCategory(category: String, pageParams: Page): PersistenceService[List[SharedCollection]] =
    PersistenceService {
      collectionPersistence.fetchList(
        sql    = Queries.getTopByCategory,
        values = (category, pageParams.pageSize, pageParams.pageNumber)
      )
    }

  override def increaseViewsByOne(id: Long): PersistenceService[Int] =
    PersistenceService {
      collectionPersistence.update(
        sql    = Queries.increaseViewsByOne,
        values = id
      )
    }

  override def update(id: Long, title: String): PersistenceService[Int] =
    PersistenceService {
      collectionPersistence.update(
        sql    = Queries.update,
        values = (title, id)
      )
    }

  override def updatePackages(collectionId: Long, packages: List[Package]): PersistenceService[(List[Package], List[Package])] = {

    def updatePackagesInfo(newPackages: List[Package], removedPackages: List[Package]) =
      if (newPackages.nonEmpty || removedPackages.nonEmpty)
        PersistenceService(collectionPersistence
          .update(Queries.updatePackages, (packages map (_.value), collectionId)))
      else
        PersistenceService(0)

    for {
      collection ← getById(collectionId).toEitherT
      existingPackages = collection.packages map Package
      newPackages = packages diff existingPackages
      removedPackages = existingPackages diff packages
      _ ← updatePackagesInfo(newPackages, removedPackages).toEitherT
    } yield (newPackages, removedPackages)
  }.value

}

object Services {

  case class SharedCollectionData(
    publicIdentifier: String,
    userId: Option[Long],
    publishedOn: Timestamp,
    author: String,
    name: String,
    views: Int,
    category: String,
    icon: String,
    community: Boolean,
    packages: List[String]
  )

  implicit val connectionIOMonad: Monad[ConnectionIO] = ScalazInstances[ConnectionIO].monadInstance

  def services(implicit collectionPersistence: Persistence[SharedCollection]) =
    new Services(collectionPersistence)
}

