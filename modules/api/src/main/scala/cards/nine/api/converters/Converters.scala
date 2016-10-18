package cards.nine.api.converters

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.messages.GooglePlayMessages._
import cards.nine.api.messages.InstallationsMessages._
import cards.nine.api.messages.SharedCollectionMessages._
import cards.nine.api.messages.UserMessages._
import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.account._
import cards.nine.domain.application.{ FullCard, FullCardList, Package }
import cards.nine.domain.market.MarketCredentials
import cards.nine.processes.messages.InstallationsMessages._
import cards.nine.processes.messages.SharedCollectionMessages._
import cards.nine.processes.messages.UserMessages._
import cards.nine.processes.messages.rankings.GetRankedDeviceApps._
import cats.syntax.either._

object Converters {

  def toLoginRequest(request: ApiLoginRequest, sessionToken: SessionToken): LoginRequest =
    LoginRequest(
      email        = request.email,
      androidId    = request.androidId,
      sessionToken = sessionToken,
      tokenId      = request.tokenId
    )

  implicit def toApiLoginResponse(response: LoginResponse): ApiLoginResponse =
    ApiLoginResponse(
      apiKey       = response.apiKey,
      sessionToken = response.sessionToken
    )

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
        installations    = request.installations,
        views            = request.views,
        category         = request.category,
        icon             = request.icon,
        community        = request.community
      ),
      packages   = request.packages
    )

  @inline
  def toApiSharedCollection(info: SharedCollectionWithAppsInfo): ApiSharedCollection =
    ApiSharedCollection(
      publicIdentifier = info.collection.publicIdentifier,
      publishedOn      = info.collection.publishedOn,
      author           = info.collection.author,
      name             = info.collection.name,
      installations    = info.collection.installations,
      views            = info.collection.views,
      category         = info.collection.category,
      icon             = info.collection.icon,
      community        = info.collection.community,
      owned            = info.collection.owned,
      packages         = info.collection.packages,
      appsInfo         = info.appsInfo map toApiCollectionApp,
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
      category    = toMainCategory(card.categories)
    )

  def toApiSharedCollectionList(response: GetCollectionsResponse): ApiSharedCollectionList =
    ApiSharedCollectionList(response.collections map toApiSharedCollection)

  def toUpdateInstallationRequest(
    request: ApiUpdateInstallationRequest,
    userContext: UserContext
  ): UpdateInstallationRequest =
    UpdateInstallationRequest(
      userId      = userContext.userId.value,
      androidId   = userContext.androidId,
      deviceToken = request.deviceToken
    )

  def toApiUpdateInstallationResponse(
    response: UpdateInstallationResponse
  ): ApiUpdateInstallationResponse =
    ApiUpdateInstallationResponse(
      androidId   = response.androidId,
      deviceToken = response.deviceToken
    )

  def toApiSubscribeResponse(response: SubscribeResponse): ApiSubscribeResponse =
    ApiSubscribeResponse()

  def toApiUnsubscribeResponse(response: UnsubscribeResponse): ApiUnsubscribeResponse =
    ApiUnsubscribeResponse()

  def toMarketAuth(googlePlayContext: GooglePlayContext, userContext: UserContext): MarketCredentials =
    MarketCredentials(
      androidId    = userContext.androidId,
      localization = googlePlayContext.marketLocalization,
      token        = googlePlayContext.googlePlayToken
    )

  def toApiCategorizeAppsResponse(response: FullCardList): ApiCategorizeAppsResponse = {
    def toCategorizedApp(appInfo: FullCard): CategorizedApp =
      CategorizedApp(
        packageName = appInfo.packageName,
        category    = toMainCategory(appInfo.categories)
      )
    ApiCategorizeAppsResponse(
      items  = response.cards map toCategorizedApp,
      errors = response.missing
    )
  }

  def toApiDetailAppsResponse(response: FullCardList): ApiDetailAppsResponse =
    ApiDetailAppsResponse(
      items  = response.cards map toApiDetailsApp,
      errors = response.missing
    )

  def toApiDetailsApp(card: FullCard): ApiDetailsApp =
    ApiDetailsApp(
      packageName = card.packageName,
      title       = card.title,
      free        = card.free,
      icon        = card.icon,
      stars       = card.stars,
      downloads   = card.downloads,
      categories  = card.categories
    )

  def toApiRecommendation(card: FullCard): ApiRecommendation =
    ApiRecommendation(
      packageName = card.packageName,
      title       = card.title,
      free        = card.free,
      icon        = card.icon,
      stars       = card.stars,
      downloads   = card.downloads,
      screenshots = card.screenshots
    )

  def toApiGetSubscriptionsByUser(response: GetSubscriptionsByUserResponse): ApiGetSubscriptionsByUser =
    ApiGetSubscriptionsByUser(
      subscriptions = response.subscriptions
    )

  def toApiGetRecommendationsResponse(response: FullCardList): ApiGetRecommendationsResponse =
    ApiGetRecommendationsResponse(
      response.cards map toApiRecommendation
    )

  def toApiSearchAppsResponse(response: FullCardList): ApiSearchAppsResponse =
    ApiSearchAppsResponse(response.cards map toApiRecommendation)

  def toApiRankAppsResponse(result: Result[Map[String, List[RankedDeviceApp]]]) =
    result.map {
      items ⇒
        ApiRankAppsResponse(items.mapValues(apps ⇒ apps.map(_.packageName)))
    }

  def toDeviceAppList(items: List[Package]) = items map DeviceApp.apply

  def toMainCategory(categories: List[String]): String = categories.headOption.getOrElse("")

}
