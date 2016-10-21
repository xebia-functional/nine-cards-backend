package cards.nine.api.converters

import cards.nine.api.messages.{ rankings ⇒ Api }
import cards.nine.domain.analytics.{ AnalyticsToken, DateRange, RankingParams }
import cards.nine.processes.messages.rankings.{ Get, Reload }

object rankings {

  def toApiRanking(resp: Get.Response): Api.Ranking = Api.Ranking(resp.ranking.categories)

  object reload {

    def toRankingParams(token: String, request: Api.Reload.Request): RankingParams = {
      val length = request.rankingLength
      val dateRange = DateRange(request.startDate, request.endDate)
      RankingParams(dateRange, length, AnalyticsToken(token))
    }

    def toXorResponse(proc: Reload.XorResponse): Api.Reload.XorResponse =
      proc.bimap(
        err ⇒ Api.Reload.Error(err.code, err.message, err.status),
        res ⇒ Api.Reload.Response()
      )
  }

}