package cards.nine.api.collections

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.collections.messages._
import cards.nine.domain.application._
import cards.nine.processes.collections.messages._

private[collections] object Converters {

  implicit def toApiCreateOrUpdateCollectionResponse(
    response: CreateOrUpdateCollectionResponse
  ): ApiCreateOrUpdateCollectionResponse =
    ApiCreateOrUpdateCollectionResponse(
      publicIdentifier = response.publicIdentifier,
      packagesStats    = response.packagesStats
    )

  def toCreateCollectionRequest(
    request: ApiCreateCollectionRequest,
    collectionInfo: NewSharedCollectionInfo,
    userContext: UserContext
  ): CreateCollectionRequest =
    CreateCollectionRequest(
      collection = SharedCollectionData(
        publicIdentifier = collectionInfo.identifier.value,
        userId           = Option(userContext.userId.value),
        publishedOn      = collectionInfo.currentDate.value,
        author           = request.author,
        name             = request.name,
        views            = request.views,
        category         = request.category,
        icon             = request.icon,
        community        = request.community,
        packages         = request.packages
      )
    )

  def toApiIncreaseViewsCountByOneResponse(
    response: IncreaseViewsCountByOneResponse
  ): ApiIncreaseViewsCountByOneResponse =
    ApiIncreaseViewsCountByOneResponse(
      publicIdentifier = response.publicIdentifier
    )

  def toApiSharedCollection[A](info: SharedCollectionWithAppsInfo[A])(toApiApp: A ⇒ ApiCollectionApp): ApiSharedCollection =
    ApiSharedCollection(
      publicIdentifier = info.collection.publicIdentifier,
      publishedOn      = info.collection.publishedOn,
      author           = info.collection.author,
      name             = info.collection.name,
      views            = info.collection.views,
      category         = info.collection.category,
      icon             = info.collection.icon,
      community        = info.collection.community,
      owned            = info.collection.owned,
      packages         = info.collection.packages,
      appsInfo         = info.appsInfo map toApiApp,
      subscriptions    = info.collection.subscriptionsCount
    )

  def toApiCollectionApp(card: FullCard): ApiCollectionApp =
    ApiCollectionApp(
      packageName = card.packageName,
      title       = card.title,
      free        = card.free,
      icon        = card.icon,
      stars       = card.stars,
      downloads   = card.downloads,
      categories  = card.categories
    )

  def toApiCollectionApp(card: BasicCard): ApiCollectionApp =
    ApiCollectionApp(
      packageName = card.packageName,
      title       = card.title,
      free        = card.free,
      icon        = card.icon,
      stars       = card.stars,
      downloads   = card.downloads,
      categories  = Nil
    )

  def toApiSharedCollectionList(response: GetCollectionsResponse): ApiSharedCollectionList =
    ApiSharedCollectionList(response.collections map { col ⇒ toApiSharedCollection(col)(toApiCollectionApp) })

  def toApiSubscribeResponse(response: SubscribeResponse): ApiSubscribeResponse =
    ApiSubscribeResponse()

  def toApiUnsubscribeResponse(response: UnsubscribeResponse): ApiUnsubscribeResponse =
    ApiUnsubscribeResponse()

  def toApiGetSubscriptionsByUser(response: GetSubscriptionsByUserResponse): ApiGetSubscriptionsByUser =
    ApiGetSubscriptionsByUser(
      subscriptions = response.subscriptions
    )

}
