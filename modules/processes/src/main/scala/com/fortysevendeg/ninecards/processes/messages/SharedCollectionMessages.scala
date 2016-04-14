package com.fortysevendeg.ninecards.processes.messages

import cats.data.Xor
import org.joda.time.DateTime

object SharedCollectionMessages {

  case class ResolvedPackageInfo(
    packageName: String,
    title: String,
    description: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String)

  case class SharedCollectionData(
    userId: Option[Long],
    description: Option[String],
    author: String,
    name: String,
    installations: Option[Int] = None,
    views: Option[Int] = None,
    category: String,
    icon: String,
    community: Boolean)

  case class SharedCollectionInfo(
    publicIdentifier: String,
    publishedOn: DateTime,
    description: Option[String],
    author: String,
    name: String,
    sharedLink: String,
    installations: Int,
    views: Int,
    category: String,
    icon: String,
    community: Boolean,
    packages: List[String],
    resolvedPackages: List[ResolvedPackageInfo])

  case class CreateCollectionRequest(
    collection: SharedCollectionData,
    packages: List[String])

  case class CreateCollectionResponse(data: SharedCollectionInfo)

  case class GetCollectionByPublicIdentifierResponse(data: SharedCollectionInfo)

  type XorGetCollectionByPublicId = Xor[Throwable, GetCollectionByPublicIdentifierResponse]
}
