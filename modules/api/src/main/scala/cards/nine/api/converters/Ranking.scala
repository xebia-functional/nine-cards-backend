package cards.nine.api.converters

import cards.nine.api.messages.{ rankings ⇒ Api }
import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.analytics.{ AnalyticsToken, DateRange, RankingParams }
import cards.nine.processes.messages.rankings.{ Get, Reload }
import cats.syntax.either._

object rankings {

  def toApiRanking(resp: Get.Response): Api.Ranking = Api.Ranking(resp.ranking.categories)

  object reload {

    def toRankingParams(token: String, request: Api.Reload.Request): RankingParams = {
      val length = request.rankingLength
      val dateRange = DateRange(request.startDate, request.endDate)
      RankingParams(dateRange, length, AnalyticsToken(token))
    }

    def toApiResponse(response: Result[Reload.Response]): Result[Api.Reload.Response] =
      response map (_ ⇒ Api.Reload.Response())
  }

}