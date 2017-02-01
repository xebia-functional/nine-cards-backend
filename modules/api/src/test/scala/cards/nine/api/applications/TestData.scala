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

import cards.nine.domain.analytics.{ RankedAppsByCategory, RankedWidgetsByMoment }
import cards.nine.domain.application.{ CardList, FullCard, Package }
import org.joda.time.DateTime

private[applications] object TestData {

  import messages._

  val author = "John Doe"

  val category = "SOCIAL"

  val community = true

  val limit = 20

  val limitPerApp = Some(25)

  val location = Option("US")

  val now = DateTime.now

  val packagesName = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany",
    "earth.europe.france",
    "earth.europe.portugal",
    "earth.europe.spain"
  ) map Package

  val deviceApps = Map("countries" → packagesName)

  val excludePackages = packagesName.filter(_.value.length > 18)

  val moments = List("HOME", "NIGHT")

  val setAppInfoRequest = ApiSetAppInfoRequest(
    title       = "App Example",
    free        = false,
    icon        = "",
    stars       = 0.0,
    downloads   = "",
    categories  = Nil,
    screenshots = Nil
  )

  val apiGetAppsInfoRequest = ApiAppsInfoRequest(items = List("", "", "") map Package)

  val apiGetRecommendationsByCategoryRequest = ApiGetRecommendationsByCategoryRequest(
    excludePackages = excludePackages,
    limit           = limit
  )

  val apiGetRecommendationsForAppsRequest = ApiGetRecommendationsForAppsRequest(
    packages        = packagesName,
    excludePackages = excludePackages,
    limit           = limit,
    limitPerApp     = limitPerApp
  )

  val apiRankAppsRequest = ApiRankAppsRequest(
    location = location,
    items    = deviceApps
  )

  val apiRankByMomentsRequest = ApiRankByMomentsRequest(
    location = location,
    items    = packagesName,
    moments  = moments,
    limit    = limit
  )

  val getRankedAppsResponse = List.empty[RankedAppsByCategory]

  val getRankedWidgetsResponse = List.empty[RankedWidgetsByMoment]

  val getRecommendationsByCategoryResponse = CardList[FullCard](Nil, Nil, Nil)

  val getAppsInfoResponse = CardList[FullCard](Nil, Nil, Nil)

  object Paths {

    val healthcheck = "/healthcheck"

    val categorize = "/applications/categorize"

    val details = "/applications/details"

    val rankApps = "/applications/rank"

    val rankAppsByMoments = "/applications/rank-by-moments"

    val recommendationsByCategory = "/recommendations/SOCIAL"

    val recommendationsForApps = "/recommendations"

    val rankWidgets = "/widgets/rank"

  }

}
