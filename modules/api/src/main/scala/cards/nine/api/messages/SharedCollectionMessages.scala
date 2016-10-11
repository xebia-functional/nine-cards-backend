package cards.nine.api.messages

import cats.data.Xor
import cards.nine.processes.messages.SharedCollectionMessages._
import org.joda.time.DateTime

object SharedCollectionMessages {

  case class ApiCreateCollectionRequest(
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
    author: String,
    name: String,
    installations: Int,
    views: Int,
    category: String,
    icon: String,
    community: Boolean,
    owned: Boolean,
    packages: List[String],
    appsInfo: List[AppInfo],
    subscriptions: Option[Long] = None
  )

  case class ApiSharedCollectionList(collections: List[ApiSharedCollection])

  case class ApiUpdateCollectionRequest(
    collectionInfo: Option[SharedCollectionUpdateInfo],
    packages: Option[List[String]]
  )

  type XorApiGetCollectionByPublicId = Xor[Throwable, ApiSharedCollection]

  case class ApiGetSubscriptionsByUser(subscriptions: List[String])

  case class ApiSubscribeResponse()

  case class ApiUnsubscribeResponse()

}
