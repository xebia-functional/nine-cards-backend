package com.fortysevendeg.ninecards.api.messages

import cats.data.Xor
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
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

  case class ApiCreateOrUpdateCollectionResponse(
    publicIdentifier: String,
    packagesStats: PackagesStats
  )

  case class ApiSharedCollection(
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
    packages: List[String],
    appsInfo: List[AppInfo]
  )

  case class ApiSharedCollectionList(collections: List[ApiSharedCollection])

  case class ApiUpdateCollectionRequest(
    collectionInfo: Option[SharedCollectionUpdateInfo],
    packages: Option[List[String]]
  )

  type XorApiGetCollectionByPublicId = Xor[Throwable, ApiSharedCollection]

  case class ApiSubscribeResponse()

  case class ApiUnsubscribeResponse()

}
