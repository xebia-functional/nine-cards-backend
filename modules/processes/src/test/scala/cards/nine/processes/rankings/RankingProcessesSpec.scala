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

import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.NineCardsErrors.{ CountryNotFound, NineCardsError }
import cards.nine.domain.analytics._
import cards.nine.processes.NineCardsServices._
import cards.nine.processes.rankings.TestData._
import cards.nine.processes.TestInterpreters
import cards.nine.processes.rankings.messages._
import cards.nine.services.free.algebra.{ CountryR, GoogleAnalytics, RankingS }
import cards.nine.services.free.domain.Ranking.CountriesWithRanking
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.matcher.{ Matcher, Matchers, XorMatchers }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import cats.free.FreeApplicative

trait RankingsProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with XorMatchers
  with TestInterpreters {

  def rightFS[A](x: A): FreeApplicative[NineCardsServices, Result[A]] = FreeApplicative.pure(Right(x))
  def leftFS[A](x: NineCardsError): FreeApplicative[NineCardsServices, Result[A]] = FreeApplicative.pure(Left(x))

  trait BasicScope extends Scope {

    implicit val analyticsServices = mock[GoogleAnalytics[NineCardsServices]]
    implicit val countryServices = mock[CountryR[NineCardsServices]]
    implicit val rankingServices = mock[RankingS[NineCardsServices]]

    val rankingProcesses = RankingProcesses.processes[NineCardsServices]

    def appsHaveAtMost(maxSize: Int): Matcher[RankedAppsByCategory] = {
      ranking: RankedAppsByCategory ⇒ ranking.packages.size must be_<=(maxSize)
    }

    def appHasRankingInfo(hasRanking: Boolean): Matcher[RankedApp] = {
      app: RankedApp ⇒ app.position.isDefined must_== hasRanking
    }

    def allAppsHaveRankingInfo(hasRanking: Boolean): Matcher[RankedAppsByCategory] = {
      ranking: RankedAppsByCategory ⇒ ranking.packages must contain(appHasRankingInfo(hasRanking)).forall
    }

    def widgetHasRankingInfo(hasRanking: Boolean): Matcher[RankedWidget] = {
      app: RankedWidget ⇒ app.position.isDefined must_== hasRanking
    }

    def widgetsHaveAtMost(maxSize: Int): Matcher[RankedWidgetsByMoment] = {
      ranking: RankedWidgetsByMoment ⇒ ranking.widgets.size must be_<=(maxSize)
    }

    def allWidgetsHaveRankingInfo(hasRanking: Boolean): Matcher[RankedWidgetsByMoment] = {
      ranking: RankedWidgetsByMoment ⇒ ranking.widgets must contain(widgetHasRankingInfo(hasRanking)).forall
    }
  }
}

class RankingsProcessesSpec extends RankingsProcessesSpecification {

  "getRanking" should {
    "give a valid ranking if the cache has info for the given scope" in new BasicScope {
      rankingServices.getRanking(scope) returns rightFS(googleAnalyticsRanking)

      rankingProcesses.getRanking(scope).foldMap(testInterpreters) must beRight[Get.Response].which {
        response ⇒
          response.ranking must_== googleAnalyticsRanking
      }
    }

    "give a RankingNotFound error if the cache has no info for the given scope" in new BasicScope {
      rankingServices.getRanking(scope) returns leftFS(rankingNotFoundError)

      rankingProcesses.getRanking(scope).foldMap(testInterpreters) must beLeft[NineCardsError]
    }
  }

  "reloadRankingByScope" should {
    "give a good answer" in new BasicScope {
      analyticsServices.getRanking(code = any, categories = any, params = mockEq(params)) returns
        rightFS(googleAnalyticsRanking)

      analyticsServices.getCountriesWithRanking(params) returns
        rightFS(CountriesWithRanking(List(CountryIsoCode(countryIsoCode2))))

      rankingServices.updateRanking(usaScope, googleAnalyticsRanking) returns
        rightFS(UpdateRankingSummary(Option(CountryIsoCode(countryIsoCode2)), 0))

      val response = rankingProcesses.reloadRankingByScope(usaScope, params)
      response.foldMap(testInterpreters) must beRight(Reload.Response())
    }
    "return a CountryNotFound error if the country doesn't have ranking info" in new BasicScope {
      analyticsServices.getRanking(code = any, categories = any, params = mockEq(params)) returns
        rightFS(googleAnalyticsRanking)

      analyticsServices.getCountriesWithRanking(params) returns
        rightFS(CountriesWithRanking(List(CountryIsoCode(countryIsoCode2))))

      val response = rankingProcesses.reloadRankingByScope(scope, params)
      response.foldMap(testInterpreters) must beLeft(CountryNotFound("The country doesn't have ranking info"))
    }
  }

  "getRankedApps" should {
    "return an empty response if no device apps are given" in new BasicScope {
      val response = rankingProcesses.getRankedDeviceApps(location, emptyUnrankedAppsMap)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]](Nil)
    }
    "return all the device apps as ranked if there is ranking info for them" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns rightFS(country)
      rankingServices.rankApps(mockEq(usaScope), any) returns rankingForAppsResponse

      val response = rankingProcesses.getRankedDeviceApps(location, unrankedAppsMap)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]].which { r ⇒
        r must contain(allAppsHaveRankingInfo(true)).forall
      }
    }
    "return all the device apps as ranked by using world ranking if an unknown country is given" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns leftFS(countryNotFoundError)
      rankingServices.rankApps(mockEq(WorldScope), any) returns rankingForAppsResponse

      val response = rankingProcesses.getRankedDeviceApps(location, unrankedAppsMap)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]].which { r ⇒
        r must contain(allAppsHaveRankingInfo(true)).forall
      }
    }
    "return all the device apps as unranked if there is no ranking info for them" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns rightFS(country)
      rankingServices.rankApps(mockEq(usaScope), any) returns rankingForAppsEmptyResponse

      val response = rankingProcesses.getRankedDeviceApps(location, unrankedAppsMap)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]].which { r ⇒
        r must contain(allAppsHaveRankingInfo(false)).forall
      }
    }

  }

  "getRankedAppsByMoment" should {
    "return an empty response if no device apps are given" in new BasicScope {
      val response = rankingProcesses.getRankedAppsByMoment(location, emptyUnrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]](Nil)
    }
    "return all the apps as ranked if there is ranking info for them" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns rightFS(country)
      rankingServices.rankAppsWithinMoments(mockEq(usaScope), any, mockEq(moments)) returns
        rankingForAppsResponse

      val response = rankingProcesses.getRankedAppsByMoment(location, unrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]].which { r ⇒
        r must contain(appsHaveAtMost(limit)).forall
        r must contain(allAppsHaveRankingInfo(true)).forall
      }
    }
    "return all the  apps as ranked by using world ranking if an unknown country is given" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns leftFS(countryNotFoundError)
      rankingServices.rankAppsWithinMoments(mockEq(WorldScope), any, mockEq(moments)) returns
        rankingForAppsResponse

      val response = rankingProcesses.getRankedAppsByMoment(location, unrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]].which { r ⇒
        r must contain(appsHaveAtMost(limit)).forall
        r must contain(allAppsHaveRankingInfo(true)).forall
      }
    }
    "return an empty response if there is no ranking info for them" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns rightFS(country)
      rankingServices.rankAppsWithinMoments(mockEq(usaScope), any, mockEq(moments)) returns
        rankingForAppsEmptyResponse

      val response = rankingProcesses.getRankedAppsByMoment(location, unrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]](Nil)
    }
  }

  "getRankedWidgets" should {
    "return an empty response if no apps are given" in new BasicScope {
      val response = rankingProcesses.getRankedWidgets(location, emptyUnrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedWidgetsByMoment]](Nil)
    }
    "return all the widgets as ranked if there is ranking info for them" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns rightFS(country)
      rankingServices.rankWidgets(mockEq(usaScope), any, mockEq(widgetMoments)) returns
        rankingForWidgetsResponse

      val response = rankingProcesses.getRankedWidgets(location, unrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedWidgetsByMoment]].which { r ⇒
        r must contain(widgetsHaveAtMost(limit)).forall
        r must contain(allWidgetsHaveRankingInfo(true)).forall
      }
    }
    "return all the widgets as ranked by using world ranking if an unknown country is given" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns leftFS(countryNotFoundError)
      rankingServices.rankWidgets(mockEq(WorldScope), any, mockEq(widgetMoments)) returns
        rankingForWidgetsResponse

      val response = rankingProcesses.getRankedWidgets(location, unrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedWidgetsByMoment]].which { r ⇒
        r must contain(widgetsHaveAtMost(limit)).forall
        r must contain(allWidgetsHaveRankingInfo(true)).forall
      }
    }
    "return all the widgets as unranked if there is no ranking info for them" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns rightFS(country)
      rankingServices.rankWidgets(mockEq(usaScope), any, mockEq(widgetMoments)) returns
        rankingForWidgetsEmptyResponse

      val response = rankingProcesses.getRankedWidgets(location, unrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedWidgetsByMoment]](Nil)
    }
  }
}
