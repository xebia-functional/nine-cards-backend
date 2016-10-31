package cards.nine.services.free.interpreter.collection

import java.sql.Timestamp

import cards.nine.domain.application.Package
import cards.nine.services.free.algebra.SharedCollection._
import cards.nine.services.free.domain.SharedCollection.{ Queries ⇒ CollectionQueries }
import cards.nine.services.free.domain.SharedCollectionPackage.{ Queries ⇒ PackageQueries }
import cards.nine.services.free.domain._
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import cards.nine.services.persistence.Persistence
import cats.~>
import doobie.imports.{ Composite, ConnectionIO }
import shapeless.syntax.std.product._

import scalaz.std.iterable._

class Services(
  collectionPersistence: Persistence[SharedCollection],
  packagePersistence: Persistence[SharedCollectionPackage]
) extends (Ops ~> ConnectionIO) {

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

  def addPackage[K: Composite](collectionId: Long, packageName: Package): ConnectionIO[K] =
    packagePersistence.updateWithGeneratedKeys[K](
      sql    = PackageQueries.insert,
      fields = SharedCollectionPackage.allFields,
      values = (collectionId, packageName)
    )

  def addPackages(collectionId: Long, packagesName: List[Package]): ConnectionIO[Int] = {
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

  def deletePackages(collectionId: Long, packagesName: List[Package]): ConnectionIO[Int] = {
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

  def updatePackages(collection: Long, packages: List[Package]): ConnectionIO[(List[Package], List[Package])] =
    for {
      oldPackages ← getPackagesByCollection(collection)
      oldPackagesNames = oldPackages map (p ⇒ Package(p.packageName))
      newPackages = packages diff oldPackagesNames
      removedPackages = oldPackagesNames diff packages
      addedPackages ← addPackages(collection, newPackages)
      deletedPackages ← deletePackages(collection, removedPackages)
    } yield (newPackages, removedPackages)

  def apply[A](fa: Ops[A]): ConnectionIO[A] = fa match {
    case Add(collection) ⇒
      add[SharedCollection](collection)
    case AddPackages(collection, packages) ⇒
      addPackages(collection, packages)
    case GetById(id) ⇒
      getById(id)
    case GetByPublicId(publicId) ⇒
      getByPublicIdentifier(publicId)
    case GetByUser(user) ⇒
      getByUser(user)
    case GetLatestByCategory(category, pageNumber, pageSize) ⇒
      getLatestByCategory(category, pageNumber, pageSize)
    case GetPackagesByCollection(collection) ⇒
      getPackagesByCollection(collection)
    case GetTopByCategory(category, pageNumber, pageSize) ⇒
      getTopByCategory(category, pageNumber, pageSize)
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
    community: Boolean
  )

  def services(
    implicit
    collectionPersistence: Persistence[SharedCollection],
    packagePersistence: Persistence[SharedCollectionPackage]
  ) =
    new Services(collectionPersistence, packagePersistence)
}

