package com.fortysevendeg.ninecards.api.messages

import cats.data.Xor
import org.joda.time.DateTime

object SharedCollectionMessages {

  case class ApiCreateCollectionRequest(
    description: Option[String],
    author: String,
    name: String,
    installations: Option[Int] = None,
    views: Option[Int] = None,
    category: String,
    icon: String,
    community: Boolean,
    packages: List[String]
  )

  case class ApiCreateCollectionResponse(
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
    packages: List[String]
  )

  case class ApiResolvedPackageInfo(
    packageName: String,
    title: String,
    description: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String
  )

  case class ApiGetCollectionByPublicIdentifierResponse(
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
    resolvedPackages: List[ApiResolvedPackageInfo]
  )

  type XorApiGetCollectionByPublicId = Xor[Throwable, ApiGetCollectionByPublicIdentifierResponse]

  case class ApiSubscribeResponse()

  type XorApiSubscribeResponse = Xor[Throwable, ApiSubscribeResponse]

  case class ApiUnsubscribeResponse()

  type XorApiUnsubscribeResponse = Xor[Throwable, ApiUnsubscribeResponse]
}
