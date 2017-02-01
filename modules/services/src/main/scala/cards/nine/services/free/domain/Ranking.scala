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
package cards.nine.services.free.domain

import cards.nine.domain.analytics._
import cards.nine.domain.analytics.ReportType.AppsRankingByCategory
import cards.nine.domain.application.Package

object Ranking {

  case class GoogleAnalyticsRanking(categories: Map[String, List[Package]]) extends AnyVal

  case class CountriesWithRanking(countries: List[CountryIsoCode]) extends AnyVal

  case class RankingError(code: Int, message: String, status: String)

  case class AppRankingInfo(packageName: Package, position: Int)

  case class CacheKey(scope: GeoScope, reportType: ReportType)

  case class CacheVal(ranking: Option[GoogleAnalyticsRanking])

  object CacheKey {

    def worldScope: CacheKey = CacheKey(WorldScope, AppsRankingByCategory)

    def countryScope(code: String): CacheKey = CacheKey(
      scope      = CountryScope(CountryIsoCode(code.toLowerCase)),
      reportType = AppsRankingByCategory
    )

  }
}
