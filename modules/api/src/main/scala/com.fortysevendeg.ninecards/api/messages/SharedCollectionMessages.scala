package com.fortysevendeg.ninecards.api.messages

import cats.data.Xor
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages.AppInfo
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

  case class ApiSharedCollection(
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
    appsInfo: List[AppInfo]
  )

  type XorApiGetCollectionByPublicId = Xor[Throwable, ApiSharedCollection]

  case class ApiSubscribeResponse()

  case class ApiUnsubscribeResponse()

}
