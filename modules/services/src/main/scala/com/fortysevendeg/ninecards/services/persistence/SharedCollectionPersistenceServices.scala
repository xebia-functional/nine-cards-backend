package com.fortysevendeg.ninecards.services.persistence

import java.sql.Timestamp

import com.fortysevendeg.ninecards.services.free.domain.SharedCollection.{ Queries ⇒ CollectionQueries }
import com.fortysevendeg.ninecards.services.free.domain.SharedCollectionPackage.{ Queries ⇒ PackageQueries }
import com.fortysevendeg.ninecards.services.free.domain._
import com.fortysevendeg.ninecards.services.persistence.SharedCollectionPersistenceServices.SharedCollectionData
import doobie.imports._
import shapeless.syntax.std.product._

import scalaz.std.iterable._

class SharedCollectionPersistenceServices(
  implicit
  collectionPersistence: Persistence[SharedCollection],
  collectionPackagePersistence: Persistence[SharedCollectionPackage]
) {

  def addCollection[K: Composite](data: SharedCollectionData): ConnectionIO[K] =
    collectionPersistence.updateWithGeneratedKeys[K](
      sql    = CollectionQueries.insert,
      fields = SharedCollection.allFields,
      values = data.toTuple
    )

  def getCollectionById(id: Long): ConnectionIO[Option[SharedCollection]] =
    collectionPersistence.fetchOption(CollectionQueries.getById, id)

  def getCollectionByPublicIdentifier(publicIdentifier: String): ConnectionIO[Option[SharedCollection]] =
    collectionPersistence.fetchOption(CollectionQueries.getByPublicIdentifier, publicIdentifier)

  def getCollectionsByUserId(userId: Long): ConnectionIO[List[SharedCollection]] =
    collectionPersistence.fetchList(CollectionQueries.getByUser, userId)

  def getLatestCollectionsByCategory(category: String): ConnectionIO[List[SharedCollection]] =
    collectionPersistence.fetchList(CollectionQueries.getLatestByCategory, category)

  def getTopCollectionsByCategory(category: String): ConnectionIO[List[SharedCollection]] =
    collectionPersistence.fetchList(CollectionQueries.getTopByCategory, category)

  def addPackage[K: Composite](collectionId: Long, packageName: String): ConnectionIO[K] =
    collectionPackagePersistence.updateWithGeneratedKeys[K](
      sql    = PackageQueries.insert,
      fields = SharedCollectionPackage.allFields,
      values = (collectionId, packageName)
    )

  def addPackages(collectionId: Long, packagesName: List[String]): ConnectionIO[Int] = {
    val packages = packagesName map {
      (collectionId, _)
    }

    collectionPackagePersistence.updateMany(
      sql    = PackageQueries.insert,
      values = packages
    )
  }

  def getPackagesByCollection(collectionId: Long): ConnectionIO[List[SharedCollectionPackage]] =
    collectionPackagePersistence.fetchList(PackageQueries.getBySharedCollection, collectionId)

  def deletePackages(collectionId: Long, packagesName: List[String]): ConnectionIO[Int] = {
    val packages = packagesName map {
      (collectionId, _)
    }

    collectionPackagePersistence.updateMany(
      sql    = PackageQueries.delete,
      values = packages
    )
  }

  def updateCollectionInfo(
    id: Long,
    title: String,
    description: Option[String]
  ): ConnectionIO[Int] =
    collectionPersistence.update(
      sql    = CollectionQueries.update,
      values = (title, description, id)
    )

  def updatePackages(collectionId: Long, packages: List[String]): ConnectionIO[(Int, Int)] =
    for {
      oldPackages ← getPackagesByCollection(collectionId)
      oldPackagesNames = oldPackages map (_.packageName)
      newPackages = packages diff oldPackagesNames
      removedPackages = oldPackagesNames diff packages
      addedPackages ← addPackages(collectionId, newPackages)
      deletedPackages ← deletePackages(collectionId, removedPackages)
    } yield (addedPackages, deletedPackages)
}

object SharedCollectionPersistenceServices {

  case class SharedCollectionData(
    publicIdentifier: String,
    userId: Option[Long],
    publishedOn: Timestamp,
    description: Option[String] = None,
    author: String,
    name: String,
    installations: Int,
    views: Int,
    category: String,
    icon: String,
    community: Boolean
  )

  implicit def sharedCollectionPersistenceServices(
    implicit
    collectionPersistence: Persistence[SharedCollection],
    collectionPackagePersistence: Persistence[SharedCollectionPackage]
  ) =
    new SharedCollectionPersistenceServices
}