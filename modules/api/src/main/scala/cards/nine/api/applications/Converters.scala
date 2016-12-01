package cards.nine.api.applications

import cards.nine.api.converters.{ Converters ⇒ BaseConverters }
import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.application._
import cards.nine.domain.analytics.RankedAppsByCategory
import cats.syntax.either._

private[applications] object Converters {

  import messages._

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

  def toApiSetAppInfoResponse(result: Unit): ApiSetAppInfoResponse =
    ApiSetAppInfoResponse()

  def toApiSearchAppsResponse(response: CardList[BasicCard]): ApiSearchAppsResponse =
    ApiSearchAppsResponse(response.cards map BaseConverters.toApiRecommendation)

  def toApiRankedAppsByCategory(ranking: RankedAppsByCategory) =
    ApiRankedAppsByCategory(ranking.category, ranking.packages map (_.packageName))

  def toApiRankAppsResponse(result: Result[List[RankedAppsByCategory]]) =
    result.map {
      items ⇒
        ApiRankAppsResponse(items map toApiRankedAppsByCategory)
    }

}
