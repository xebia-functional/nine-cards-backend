package com.fortysevendeg.ninecards.processes.messages

import cats.data.Xor
import org.joda.time.DateTime

object SharedCollectionMessages {

  case class AppInfo(
    packageName: String,
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    category: String
  )

  case class SharedCollectionData(
    publicIdentifier: String,
    userId: Option[Long],
    publishedOn: DateTime,
    description: Option[String],
    author: String,
    name: String,
    installations: Option[Int] = None,
    views: Option[Int] = None,
    category: String,
    icon: String,
    community: Boolean
  )

  case class SharedCollectionWithAppsInfo(
    collection: SharedCollection,
    appsInfo: List[AppInfo]
  )

  case class SharedCollection(
    publicIdentifier: String,
    publishedOn: DateTime,
    description: Option[String],
    author: String,
    name: String,
    installations: Int,
    views: Int,
    category: String,
    icon: String,
    community: Boolean,
    packages: List[String]
  )

  case class SharedCollectionUpdateInfo(
    description: Option[String],
    title: String
  )

  case class CreateCollectionRequest(
    collection: SharedCollectionData,
    packages: List[String]
  )

  case class PackagesInfo(added: Int, removed: Option[Int] = None)

  case class CreateOrUpdateCollectionResponse(
    publicIdentifier: String,
    packagesInfo: PackagesInfo
  )

  case class GetCollectionByPublicIdentifierResponse(data: SharedCollectionWithAppsInfo)

  case class SubscribeResponse()

  case class UnsubscribeResponse()

  case class GetPublishedCollectionsResponse(collections: List[SharedCollectionWithAppsInfo])

  type XorGetCollectionByPublicId = Xor[Throwable, GetCollectionByPublicIdentifierResponse]

}
