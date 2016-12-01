package cards.nine.api.rankings

import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.analytics.{ AnalyticsToken, DateRange, RankingParams }
import cards.nine.processes.rankings.{ messages ⇒ Proc }
import cats.syntax.either._

object Converters {

  import messages._

  def toApiRanking(response: Result[Proc.Get.Response]): Result[Ranking] =
    response map (r ⇒ Ranking(r.ranking.categories))

  def toRankingParams(token: String, request: Reload.Request): RankingParams = {
    val length = request.rankingLength
    val dateRange = DateRange(request.startDate, request.endDate)
    RankingParams(dateRange, length, AnalyticsToken(token))
  }

  def toApiReloadResponse(response: Result[Proc.Reload.Response]): Result[Reload.Response] =
    response map (_ ⇒ Reload.Response())

}
