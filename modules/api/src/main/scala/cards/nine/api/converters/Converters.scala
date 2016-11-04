package cards.nine.api.converters

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.messages.GooglePlayMessages._
import cards.nine.api.messages.InstallationsMessages._
import cards.nine.api.messages.SharedCollectionMessages._
import cards.nine.api.messages.UserMessages._
import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.account._
import cards.nine.domain.analytics.{ RankedAppsByCategory, RankedWidgetsByMoment }
import cards.nine.domain.application.{ BasicCard, BasicCardList, FullCard, FullCardList, Package }
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

  def toApiSharedCollection[A](info: SharedCollectionWithAppsInfo[A])(toApiApp: A ⇒ ApiCollectionApp): ApiSharedCollection =
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

  def toApiAppsInfoResponse[A](toA: FullCard ⇒ A)(response: FullCardList): ApiAppsInfoResponse[A] =
    ApiAppsInfoResponse(
      errors = response.missing,
      items  = response.cards map toA
    )

  def toApiCategorizedApp(appInfo: FullCard): ApiCategorizedApp =
    ApiCategorizedApp(
      packageName = appInfo.packageName,
      categories  = appInfo.categories
    )

  def toApiIconApp(appInfo: FullCard): ApiIconApp =
    ApiIconApp(
      packageName = appInfo.packageName,
      title       = appInfo.title,
      icon        = appInfo.icon
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

  def toApiRecommendation(card: BasicCard): ApiRecommendation =
    ApiRecommendation(
      packageName = card.packageName,
      title       = card.title,
      free        = card.free,
      icon        = card.icon,
      stars       = card.stars,
      downloads   = card.downloads,
      screenshots = Nil
    )

  def toApiGetSubscriptionsByUser(response: GetSubscriptionsByUserResponse): ApiGetSubscriptionsByUser =
    ApiGetSubscriptionsByUser(
      subscriptions = response.subscriptions
    )

  def toApiGetRecommendationsResponse(response: FullCardList): ApiGetRecommendationsResponse =
    ApiGetRecommendationsResponse(
      response.cards map toApiRecommendation
    )

  def toApiSearchAppsResponse(response: BasicCardList): ApiSearchAppsResponse =
    ApiSearchAppsResponse(response.cards map toApiRecommendation)

  def toApiRankedAppsByCategory(ranking: RankedAppsByCategory) =
    ApiRankedAppsByCategory(ranking.category, ranking.packages map (_.packageName))

  def toApiRankAppsResponse(result: Result[List[RankedAppsByCategory]]) =
    result.map {
      items ⇒
        ApiRankAppsResponse(items map toApiRankedAppsByCategory)
    }

  def toApiRankedWidgetsByMoment(ranking: RankedWidgetsByMoment) =
    ApiRankedWidgetsByMoment(ranking.moment, ranking.widgets map (_.widget))

  def toApiRankWidgetsResponse(result: Result[List[RankedWidgetsByMoment]]) =
    result.map {
      items ⇒
        ApiRankWidgetsResponse(items map toApiRankedWidgetsByMoment)
    }

  def toDeviceAppList(items: List[Package]) = items map DeviceApp.apply

}
