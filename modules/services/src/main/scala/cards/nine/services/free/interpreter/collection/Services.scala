package cards.nine.services.free.interpreter.collection

import java.sql.Timestamp

import cards.nine.commons.NineCardsErrors.SharedCollectionNotFound
import cards.nine.domain.application.Package
import cards.nine.domain.pagination.Page
import cards.nine.services.common.ConnectionIOInstances._
import cards.nine.services.common.PersistenceService
import cards.nine.services.common.PersistenceService._
import cards.nine.services.free.algebra.SharedCollection._
import cards.nine.services.free.domain.SharedCollection.Queries
import cards.nine.services.free.domain._
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import cards.nine.services.persistence.Persistence
import cats.syntax.either._
import cats.~>
import doobie.contrib.postgresql.pgtypes._
import doobie.imports.{ Composite, ConnectionIO }
import shapeless.syntax.std.product._

class Services(
  collectionPersistence: Persistence[SharedCollection]
) extends (Ops ~> ConnectionIO) {

  def add[K: Composite](data: SharedCollectionData): PersistenceService[K] =
    PersistenceService {
      collectionPersistence.updateWithGeneratedKeys[K](
        sql    = Queries.insert,
        fields = SharedCollection.allFields,
        values = data.toTuple
      )
    }

  def getById(id: Long): PersistenceService[SharedCollection] =
    collectionPersistence.fetchOption(Queries.getById, id) map (
      Either.fromOption(_, SharedCollectionNotFound("Shared collection not found"))
    )

  def getByPublicIdentifier(publicIdentifier: String): PersistenceService[SharedCollection] =
    collectionPersistence.fetchOption(
      sql    = Queries.getByPublicIdentifier,
      values = publicIdentifier
    ) map (
      Either.fromOption(_, SharedCollectionNotFound(s"Shared collection with public identifier $publicIdentifier doesn't exist"))
    )

  def getByUser(user: Long): PersistenceService[List[SharedCollectionWithAggregatedInfo]] =
    PersistenceService {
      collectionPersistence.fetchListAs[SharedCollectionWithAggregatedInfo](
        sql    = Queries.getByUser,
        values = user
      )
    }

  def getLatestByCategory(category: String, pageParams: Page): PersistenceService[List[SharedCollection]] =
    PersistenceService {
      collectionPersistence.fetchList(
        sql    = Queries.getLatestByCategory,
        values = (category, pageParams.pageSize, pageParams.pageNumber)
      )
    }

  def getTopByCategory(category: String, pageParams: Page): PersistenceService[List[SharedCollection]] =
    PersistenceService {
      collectionPersistence.fetchList(
        sql    = Queries.getTopByCategory,
        values = (category, pageParams.pageSize, pageParams.pageNumber)
      )
    }

  def updateCollectionInfo(id: Long, title: String): PersistenceService[Int] =
    PersistenceService {
      collectionPersistence.update(
        sql    = Queries.update,
        values = (title, id)
      )
    }

  def updatePackages(collectionId: Long, packages: List[Package]): PersistenceService[(List[Package], List[Package])] = {

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

  def apply[A](fa: Ops[A]): ConnectionIO[A] = fa match {
    case Add(collection) ⇒
      add[SharedCollection](collection)
    case GetById(id) ⇒
      getById(id)
    case GetByPublicId(publicId) ⇒
      getByPublicIdentifier(publicId)
    case GetByUser(user) ⇒
      getByUser(user)
    case GetLatestByCategory(category, paginationParams) ⇒
      getLatestByCategory(category, paginationParams)
    case GetTopByCategory(category, pageParams) ⇒
      getTopByCategory(category, pageParams)
    case Update(id, title) ⇒
      updateCollectionInfo(id, title)
    case UpdatePackages(collection, packages) ⇒
      updatePackages(collection, packages)
  }
}

object Services {

  case class SharedCollectionData(
    publicIdentifier: String,
    userId: Option[Long],
    publishedOn: Timestamp,
    author: String,
    name: String,
    installations: Int,
    views: Int,
    category: String,
    icon: String,
    community: Boolean,
    packages: List[String]
  )

  def services(
    implicit
    collectionPersistence: Persistence[SharedCollection]
  ) = new Services(collectionPersistence)
}

