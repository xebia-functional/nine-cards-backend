package cards.nine.services.free.interpreter.collection

import java.sql.Timestamp

import cards.nine.domain.application.Package
import cards.nine.domain.pagination.Page
import cards.nine.services.free.algebra.SharedCollection._
import cards.nine.services.free.domain.SharedCollection.Queries
import cards.nine.services.free.domain._
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import cards.nine.services.persistence.Persistence
import cats.~>
import doobie.contrib.postgresql.pgtypes._
import doobie.imports.{ Composite, ConnectionIO }
import shapeless.syntax.std.product._
import scalaz.Scalaz._

class Services(
  collectionPersistence: Persistence[SharedCollection]
) extends (Ops ~> ConnectionIO) {

  def add[K: Composite](data: SharedCollectionData): ConnectionIO[K] =
    collectionPersistence.updateWithGeneratedKeys[K](
      sql    = Queries.insert,
      fields = SharedCollection.allFields,
      values = data.toTuple
    )

  def getById(id: Long): ConnectionIO[Option[SharedCollection]] =
    collectionPersistence.fetchOption(Queries.getById, id)

  def getByPublicIdentifier(publicIdentifier: String): ConnectionIO[Option[SharedCollection]] =
    collectionPersistence.fetchOption(Queries.getByPublicIdentifier, publicIdentifier)

  def getByUser(user: Long): ConnectionIO[List[SharedCollectionWithAggregatedInfo]] =
    collectionPersistence.fetchListAs(Queries.getByUser, user)

  def getLatestByCategory(
    category: String,
    pageParams: Page
  ): ConnectionIO[List[SharedCollection]] =
    collectionPersistence.fetchList(
      sql    = Queries.getLatestByCategory,
      values = (category, pageParams.pageSize, pageParams.pageNumber)
    )

  def getTopByCategory(
    category: String,
    pageParams: Page
  ): ConnectionIO[List[SharedCollection]] =
    collectionPersistence.fetchList(
      sql    = Queries.getTopByCategory,
      values = (category, pageParams.pageSize, pageParams.pageNumber)
    )

  def updateCollectionInfo(
    id: Long,
    title: String
  ): ConnectionIO[Int] =
    collectionPersistence.update(
      sql    = Queries.update,
      values = (title, id)
    )

  def updatePackages(collectionId: Long, packages: List[Package]): ConnectionIO[(List[Package], List[Package])] =
    collectionPersistence.fetchOption(Queries.getById, collectionId) flatMap {
      case None => (List.empty[Package], List.empty[Package]).point[ConnectionIO]
      case Some(collection) =>
        val existingPackages = collection.packages map Package
        val newPackages = packages diff existingPackages
        val removedPackages = existingPackages diff packages

        collectionPersistence.update(Queries.updatePackages, (packages map (_.value), collectionId)) map {
          _ => (newPackages, removedPackages)
        }
    }

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

