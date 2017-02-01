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
package cards.nine.api.applications

import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.analytics.{ RankedAppsByCategory, RankedWidgetsByMoment }
import cards.nine.domain.application._
import cards.nine.processes.rankings.messages.GetRankedDeviceApps._
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
    ApiSearchAppsResponse(response.cards map toApiRecommendation)

  def toApiRankedAppsByCategory(ranking: RankedAppsByCategory) =
    ApiRankedAppsByCategory(ranking.category, ranking.packages map (_.packageName))

  def toApiRankAppsResponse(result: Result[List[RankedAppsByCategory]]) =
    result.map {
      items ⇒
        ApiRankAppsResponse(items map toApiRankedAppsByCategory)
    }

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

  def toApiCategorizedApps(response: CardList[FullCard]): ApiCategorizedApps =
    ApiCategorizedApps(
      errors  = response.missing,
      pending = response.pending,
      items   = response.cards.map(toApiCategorizedApp)
    )

}
