/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
