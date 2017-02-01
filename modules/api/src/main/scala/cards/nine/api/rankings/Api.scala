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

import cards.nine.api.utils.SprayMatchers._
import cards.nine.api.NineCardsHeaders
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.domain.analytics._
import cards.nine.processes._
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.rankings.RankingProcesses
import cards.nine.processes.NineCardsServices._
import scala.concurrent.ExecutionContext
import spray.routing._

class RankingsApi(
  implicit
  config: NineCardsConfiguration,
  accountProcesses: AccountProcesses[NineCardsServices],
  rankingProcesses: RankingProcesses[NineCardsServices],
  executionContext: ExecutionContext
) {

  import messages._
  import Directives._

  import NineCardsMarshallers._
  import Converters._

  lazy val route: Route =
    pathPrefix("rankings") {
      geographicScope { scope ⇒
        get {
          complete(getRanking(scope))
        } ~
          post {
            reloadParams(params ⇒ complete(reloadRanking(scope, params)))
          }
      }
    }

  private type NineCardsServed[A] = cats.free.Free[NineCardsServices, A]

  private[this] lazy val geographicScope: Directive1[GeoScope] = {
    val country: Directive1[GeoScope] =
      path("countries" / TypedSegment[CountryIsoCode])
        .map(c ⇒ CountryScope(c): GeoScope)
    val world = path("world") & provide(WorldScope: GeoScope)

    world | country
  }

  private[this] lazy val reloadParams: Directive1[RankingParams] =
    for {
      authToken ← headerValueByName(NineCardsHeaders.headerGoogleAnalyticsToken)
      apiRequest ← entity(as[Reload.Request])
    } yield toRankingParams(authToken, apiRequest)

  private[this] def reloadRanking(
    scope: GeoScope,
    params: RankingParams
  ): NineCardsServed[Result[Reload.Response]] =
    rankingProcesses.reloadRankingByScope(scope, params).map(toApiReloadResponse)

  private[this] def getRanking(scope: GeoScope): NineCardsServed[Result[Ranking]] =
    rankingProcesses.getRanking(scope).map(toApiRanking)

}
