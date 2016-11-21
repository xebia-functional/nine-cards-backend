package cards.nine.api.messages

import cats.data.Xor
import cards.nine.domain.application.Package
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
    packages: List[Package]
  )

  case class ApiCreateOrUpdateCollectionResponse(
    publicIdentifier: String,
    packagesStats: PackagesStats
  )

  case class ApiIncreaseViewsCountByOneResponse(
    publicIdentifier: String
  )

  case class ApiSharedCollection(
    publicIdentifier: String,
    publishedOn: DateTime,
    author: String,
    name: String,
    views: Int,
    category: String,
    icon: String,
    community: Boolean,
    owned: Boolean,
    packages: List[Package],
    appsInfo: List[ApiCollectionApp],
    subscriptions: Option[Long] = None
  )

  // AppCollectionApp: FullCard without Screenshots
  case class ApiCollectionApp(
    packageName: Package,
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    categories: List[String]
  )

  case class ApiSharedCollectionList(collections: List[ApiSharedCollection])

  case class ApiUpdateCollectionRequest(
    collectionInfo: Option[SharedCollectionUpdateInfo],
    packages: Option[List[Package]]
  )

  type XorApiGetCollectionByPublicId = Xor[Throwable, ApiSharedCollection]

  case class ApiGetSubscriptionsByUser(subscriptions: List[String])

  case class ApiSubscribeResponse()

  case class ApiUnsubscribeResponse()

}
