package cards.nine.api.converters

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.messages.GooglePlayMessages._
import cards.nine.api.messages.InstallationsMessages._
import cards.nine.api.messages.UserMessages._
import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.account._
import cards.nine.domain.analytics.{ RankedAppsByCategory, RankedWidgetsByMoment }
import cards.nine.domain.application._
import cards.nine.domain.market.MarketCredentials
import cards.nine.processes.messages.InstallationsMessages._
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

  def toFullCard(packageName: Package, apiDetails: ApiSetAppInfoRequest): FullCard =
    FullCard(
      packageName = packageName,
      title       = apiDetails.title,
      free        = apiDetails.free,
      icon        = apiDetails.icon,
      stars       = apiDetails.stars,
      downloads   = apiDetails.downloads,
      categories  = apiDetails.categories,
      screenshots = apiDetails.screenshots
    )

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

  def toMarketAuth(googlePlayContext: GooglePlayContext, userContext: UserContext): MarketCredentials =
    MarketCredentials(
      androidId    = userContext.androidId,
      localization = googlePlayContext.marketLocalization,
      token        = googlePlayContext.googlePlayToken
    )

  def toApiAppsInfoResponse[Card, A](toA: Card ⇒ A)(response: CardList[Card]): ApiAppsInfoResponse[A] =
    ApiAppsInfoResponse(
      errors = response.missing,
      items  = response.cards map toA
    )

  def toApiCategorizedApp(appInfo: FullCard): ApiCategorizedApp =
    ApiCategorizedApp(
      packageName = appInfo.packageName,
      categories  = appInfo.categories
    )

  def toApiIconApp(appInfo: BasicCard): ApiIconApp =
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

  def toApiGetRecommendationsResponse(response: CardList[FullCard]): ApiGetRecommendationsResponse =
    ApiGetRecommendationsResponse(
      response.cards map toApiRecommendation
    )

  def toApiSearchAppsResponse(response: CardList[BasicCard]): ApiSearchAppsResponse =
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

  def toApiSetAppInfoResponse(result: Unit): ApiSetAppInfoResponse =
    ApiSetAppInfoResponse()

}
