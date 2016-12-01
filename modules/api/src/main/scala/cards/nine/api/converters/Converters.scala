package cards.nine.api.converters

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.messages.GooglePlayMessages._
import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.analytics.RankedWidgetsByMoment
import cards.nine.domain.application._
import cards.nine.domain.market.MarketCredentials
import cards.nine.processes.messages.rankings.GetRankedDeviceApps._
import cats.syntax.either._

object Converters {

  def toMarketAuth(googlePlayContext: GooglePlayContext, userContext: UserContext): MarketCredentials =
    MarketCredentials(
      androidId    = userContext.androidId,
      localization = googlePlayContext.marketLocalization,
      token        = googlePlayContext.googlePlayToken
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

  def toApiRankedWidgetsByMoment(ranking: RankedWidgetsByMoment) =
    ApiRankedWidgetsByMoment(ranking.moment, ranking.widgets map (_.widget))

  def toApiRankWidgetsResponse(result: Result[List[RankedWidgetsByMoment]]) =
    result.map {
      items ⇒
        ApiRankWidgetsResponse(items map toApiRankedWidgetsByMoment)
    }

  def toDeviceAppList(items: List[Package]) = items map DeviceApp.apply

}
