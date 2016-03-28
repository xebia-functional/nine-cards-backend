package com.fortysevendeg.ninecards.services.persistence

import java.sql.Timestamp

import com.fortysevendeg.ninecards.services.free.domain.SharedCollection.{Queries => CollectionQueries}
import com.fortysevendeg.ninecards.services.free.domain.SharedCollectionPackage.{Queries => PackageQueries}
import com.fortysevendeg.ninecards.services.free.domain._
import com.fortysevendeg.ninecards.services.persistence.SharedCollectionPersistenceServices.SharedCollectionData
import com.fortysevendeg.ninecards.services.common.TupleGeneric
import doobie.imports._
import org.joda.time.DateTime

import scalaz.std.iterable._

class SharedCollectionPersistenceServices(
  implicit collectionPersistence: Persistence[SharedCollection],
  collectionPackagePersistence: Persistence[SharedCollectionPackage]) {

  def addCollection[K](
    data: SharedCollectionData)(implicit ev: Composite[K]): ConnectionIO[K] =
    collectionPersistence.updateWithGeneratedKeys[K](
      sql = CollectionQueries.insert,
      fields = SharedCollection.allFields,
      values = TupleGeneric[SharedCollectionData].to(data))

  def getCollectionById(
    id: Long): ConnectionIO[Option[SharedCollection]] =
    collectionPersistence.fetchOption(CollectionQueries.getById, id)

  def getCollectionByPublicIdentifier(
    publicIdentifier: String): ConnectionIO[Option[SharedCollection]] =
    collectionPersistence.fetchOption(CollectionQueries.getByPublicIdentifier, publicIdentifier)

  def addPackage[K](
    collectionId: Long,
    packageName: String)(implicit ev: Composite[K]): ConnectionIO[K] =
    collectionPackagePersistence.updateWithGeneratedKeys[K](
      sql = PackageQueries.insert,
      fields = SharedCollectionPackage.allFields,
      values = (collectionId, packageName))

  def addPackages(data: List[(Long, String)]): ConnectionIO[Int] =
    collectionPackagePersistence.updateMany(
      sql = PackageQueries.insert,
      values = data)

  def getPackagesByCollection(
    collectionId: Long): ConnectionIO[List[SharedCollectionPackage]] =
    collectionPackagePersistence.fetchList(PackageQueries.getBySharedCollection, collectionId)
}

object SharedCollectionPersistenceServices {

  case class SharedCollectionData(
    publicIdentifier: String,
    userId: Option[Long],
    publishedOn: Timestamp,
    description: Option[String],
    author: String,
    name: String,
    installations: Int,
    views: Int,
    category: String,
    icon: String,
    community: Boolean)

  implicit def sharedCollectionPersistenceServices(
    implicit collectionPersistence: Persistence[SharedCollection],
    collectionPackagePersistence: Persistence[SharedCollectionPackage]) =
    new SharedCollectionPersistenceServices
}