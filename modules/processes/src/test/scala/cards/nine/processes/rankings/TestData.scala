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
package cards.nine.processes.rankings

import cards.nine.commons.NineCardsErrors.{ CountryNotFound, RankingNotFound }
import cards.nine.commons.NineCardsService
import cards.nine.domain.analytics._
import cards.nine.domain.application.{ Package, Widget }
import cards.nine.processes.NineCardsServices.NineCardsServices
import cards.nine.services.free.domain.Ranking.GoogleAnalyticsRanking
import cards.nine.services.free.domain.Country
import org.joda.time.{ DateTime, DateTimeZone }

private[rankings] object TestData {

  val canonicalId = 0

  val countryContinent = "Americas"

  val countryIsoCode2 = "US"

  val countryIsoCode3 = "USA"

  val countryName = "United States"

  val failure = 0

  val success = 1

  val country = Country(
    isoCode2  = countryIsoCode2,
    isoCode3  = Option(countryIsoCode3),
    name      = countryName,
    continent = countryContinent
  )

  val countryNotFoundError = CountryNotFound(s"Country with ISO code2 US doesn't exist")

  lazy val limit = 2
  lazy val location = Option("US")
  lazy val moments = List("HOME", "NIGHT")
  lazy val widgetMoments = moments map Converters.toWidgetMoment
  lazy val params = RankingParams(DateRange(startDate, endDate), 5, AnalyticsToken("auth_token"))
  lazy val scope = CountryScope(CountryIsoCode("ES"))
  lazy val usaScope = CountryScope(CountryIsoCode("US"))
  lazy val startDate = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC)
  lazy val endDate = new DateTime(2016, 2, 1, 0, 0, DateTimeZone.UTC)
  lazy val googleAnalyticsRanking = GoogleAnalyticsRanking(
    Map("SOCIAL" → List(Package("socialite"), Package("socialist")))
  )

  val rankingNotFoundError = RankingNotFound("Ranking not found for the scope")

  val emptyRankedAppsMap = Map.empty[String, List[RankedApp]]

  val emptyRankedWidgetsMap = Map.empty[String, List[RankedWidget]]

  val emptyUnrankedAppsList = List.empty[Package]

  val emptyUnrankedAppsMap = Map.empty[String, List[Package]]

  val emptyRankedAppsList = List.empty[RankedApp]

  val countriesAMCategory = "CountriesA-M"

  val countriesNZCategory = "CountriesN-Z"

  val countriesAMList = List(
    "earth.europe.france",
    "earth.europe.germany",
    "earth.europe.italy"
  ) map Package

  val countriesNZList = List(
    "earth.europe.portugal",
    "earth.europe.spain",
    "earth.europe.unitedKingdom"
  ) map Package

  val unrankedAppsList = countriesAMList ++ countriesNZList

  val unrankedAppsMap = Map(
    countriesAMCategory → countriesAMList,
    countriesNZCategory → countriesNZList
  )

  def appsWithRanking(apps: List[Package], category: String) =
    apps.zipWithIndex.map {
      case (packageName: Package, rank: Int) ⇒
        RankedApp(packageName, category, Option(rank))
    }

  val rankingForAppsResponse = NineCardsService.right[NineCardsServices, List[RankedApp]] {
    appsWithRanking(countriesAMList, countriesAMCategory) ++
      appsWithRanking(countriesNZList, countriesNZCategory)
  }

  val rankingForAppsEmptyResponse = NineCardsService.right[NineCardsServices, List[RankedApp]] {
    Nil
  }

  def widgetsWithRanking(apps: List[Package], category: String) =
    apps.zipWithIndex.map {
      case (packageName: Package, rank: Int) ⇒
        RankedWidget(Widget(packageName, "className"), category, Option(rank))
    }

  val rankingForWidgetsResponse = NineCardsService.right[NineCardsServices, List[RankedWidget]] {
    widgetsWithRanking(countriesAMList, countriesAMCategory) ++
      widgetsWithRanking(countriesNZList, countriesNZCategory)
  }

  val rankingForWidgetsEmptyResponse = NineCardsService.right[NineCardsServices, List[RankedWidget]] {
    Nil
  }

}
