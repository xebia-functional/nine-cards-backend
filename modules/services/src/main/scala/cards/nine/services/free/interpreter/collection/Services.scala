package cards.nine.services.free.interpreter.collection

import java.sql.Timestamp

import cards.nine.services.free.domain.SharedCollection.{ Queries ⇒ CollectionQueries }
import cards.nine.services.free.domain.SharedCollectionPackage.{ Queries ⇒ PackageQueries }
import cards.nine.services.free.domain._
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import cards.nine.services.persistence.Persistence
import cats.data.OptionT
import doobie.imports._
import shapeless.syntax.std.product._

import scalaz.std.iterable._

class Services(
  collectionPersistence: Persistence[SharedCollection],
  packagePersistence: Persistence[SharedCollectionPackage]
) {
  def add[K: Composite](data: SharedCollectionData): ConnectionIO[K] =
    collectionPersistence.updateWithGeneratedKeys[K](
      sql    = CollectionQueries.insert,
      fields = SharedCollection.allFields,
      values = data.toTuple
    )

  def getById(id: Long): ConnectionIO[Option[SharedCollection]] =
    collectionPersistence.fetchOption(CollectionQueries.getById, id)

  def getByPublicIdentifier(publicIdentifier: String): ConnectionIO[Option[SharedCollection]] =
    collectionPersistence.fetchOption(CollectionQueries.getByPublicIdentifier, publicIdentifier)

  def getByUser(user: Long): ConnectionIO[List[SharedCollectionWithAggregatedInfo]] =
    collectionPersistence.fetchListAs(CollectionQueries.getByUser, user)

  def getLatestByCategory(
    category: String,
    pageNumber: Int,
    pageSize: Int
  ): ConnectionIO[List[SharedCollection]] =
    collectionPersistence.fetchList(CollectionQueries.getLatestByCategory, (category, pageSize, pageNumber))

  def getTopByCategory(
    category: String,
    pageNumber: Int,
    pageSize: Int
  ): ConnectionIO[List[SharedCollection]] =
    collectionPersistence.fetchList(CollectionQueries.getTopByCategory, (category, pageSize, pageNumber))

  def addPackage[K: Composite](collectionId: Long, packageName: String): ConnectionIO[K] =
    packagePersistence.updateWithGeneratedKeys[K](
      sql    = PackageQueries.insert,
      fields = SharedCollectionPackage.allFields,
      values = (collectionId, packageName)
    )

  def addPackages(collectionId: Long, packagesName: List[String]): ConnectionIO[Int] = {
    val packages = packagesName map {
      (collectionId, _)
    }

    packagePersistence.updateMany(
      sql    = PackageQueries.insert,
      values = packages
    )
  }

  def getPackagesByCollection(collectionId: Long): ConnectionIO[List[SharedCollectionPackage]] =
    packagePersistence.fetchList(PackageQueries.getBySharedCollection, collectionId)

  def deletePackages(collectionId: Long, packagesName: List[String]): ConnectionIO[Int] = {
    val packages = packagesName map {
      (collectionId, _)
    }

    packagePersistence.updateMany(
      sql    = PackageQueries.delete,
      values = packages
    )
  }

  def updateCollectionInfo(
    id: Long,
    title: String
  ): ConnectionIO[Int] =
    collectionPersistence.update(
      sql    = CollectionQueries.update,
      values = (title, id)
    )

  def updatePackages(collection: Long, packages: List[String]): ConnectionIO[(List[String], List[String])] =
    for {
      oldPackages ← getPackagesByCollection(collection)
      oldPackagesNames = oldPackages map (_.packageName)
      newPackages = packages diff oldPackagesNames
      removedPackages = oldPackagesNames diff packages
      addedPackages ← addPackages(collection, newPackages)
      deletedPackages ← deletePackages(collection, removedPackages)
    } yield (newPackages, removedPackages)
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
    community: Boolean
  )

  def services(
    implicit
    collectionPersistence: Persistence[SharedCollection],
    packagePersistence: Persistence[SharedCollectionPackage]
  ) =
    new Services(collectionPersistence, packagePersistence)
}

