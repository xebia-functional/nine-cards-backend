package cards.nine.processes

import cards.nine.commons.NineCardsErrors.NineCardsError
import cards.nine.commons.NineCardsService
import cards.nine.domain.analytics._
import cards.nine.processes.NineCardsServices._
import cards.nine.processes.TestData.Values._
import cards.nine.processes.TestData.rankings._
import cards.nine.processes.messages.rankings._
import cards.nine.services.free.algebra.{ Country, GoogleAnalytics, Ranking }
import cards.nine.services.free.domain.Ranking.UpdateRankingSummary
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.matcher.{ Matcher, Matchers, XorMatchers }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait RankingsProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with XorMatchers
  with TestInterpreters {

  trait BasicScope extends Scope {

    implicit val analyticsServices: GoogleAnalytics.Services[NineCardsServices] =
      mock[GoogleAnalytics.Services[NineCardsServices]]
    implicit val countryServices: Country.Services[NineCardsServices] =
      mock[Country.Services[NineCardsServices]]
    implicit val rankingServices: Ranking.Services[NineCardsServices] =
      mock[Ranking.Services[NineCardsServices]]

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
      rankingServices.getRanking(scope) returns NineCardsService.right(googleAnalyticsRanking)

      rankingProcesses.getRanking(scope).foldMap(testInterpreters) must beRight[Get.Response].which {
        response ⇒
          response.ranking must_== googleAnalyticsRanking
      }
    }

    "give a RankingNotFound error if the cache has no info for the given scope" in new BasicScope {
      rankingServices.getRanking(scope) returns NineCardsService.left(rankingNotFoundError)

      rankingProcesses.getRanking(scope).foldMap(testInterpreters) must beLeft[NineCardsError]
    }
  }

  "reloadRanking" should {
    "give a good answer" in new BasicScope {
      analyticsServices.getRanking(name = any, params = mockEq(params)) returns
        NineCardsService.right(googleAnalyticsRanking)

      countryServices.getCountryByIsoCode2(any) returns NineCardsService.right(country)

      rankingServices.updateRanking(scope, googleAnalyticsRanking) returns
        NineCardsService.right(UpdateRankingSummary(0, 0))

      val response = rankingProcesses.reloadRanking(scope, params)
      response.foldMap(testInterpreters) must beRight(Reload.Response())
    }
  }

  "getRankedApps" should {
    "return an empty response if no device apps are given" in new BasicScope {
      val response = rankingProcesses.getRankedDeviceApps(location, emptyUnrankedAppsMap)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]](Nil)
    }
    "return all the device apps as ranked if there is ranking info for them" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns NineCardsService.right(country)
      rankingServices.getRankingForApps(mockEq(usaScope), any) returns rankingForAppsResponse

      val response = rankingProcesses.getRankedDeviceApps(location, unrankedAppsMap)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]].which { r ⇒
        r must contain(allAppsHaveRankingInfo(true)).forall
      }
    }
    "return all the device apps as ranked by using world ranking if an unknown country is given" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns NineCardsService.left(countryNotFoundError)
      rankingServices.getRankingForApps(mockEq(WorldScope), any) returns rankingForAppsResponse

      val response = rankingProcesses.getRankedDeviceApps(location, unrankedAppsMap)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]].which { r ⇒
        r must contain(allAppsHaveRankingInfo(true)).forall
      }
    }
    "return all the device apps as unranked if there is no ranking info for them" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns NineCardsService.right(country)
      rankingServices.getRankingForApps(mockEq(usaScope), any) returns rankingForAppsEmptyResponse

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
      countryServices.getCountryByIsoCode2("US") returns NineCardsService.right(country)
      rankingServices.getRankingForAppsWithinMoments(mockEq(usaScope), any, mockEq(moments)) returns
        rankingForAppsResponse

      val response = rankingProcesses.getRankedAppsByMoment(location, unrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]].which { r ⇒
        r must contain(appsHaveAtMost(limit)).forall
        r must contain(allAppsHaveRankingInfo(true)).forall
      }
    }
    "return all the  apps as ranked by using world ranking if an unknown country is given" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns NineCardsService.left(countryNotFoundError)
      rankingServices.getRankingForAppsWithinMoments(mockEq(WorldScope), any, mockEq(moments)) returns
        rankingForAppsResponse

      val response = rankingProcesses.getRankedAppsByMoment(location, unrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedAppsByCategory]].which { r ⇒
        r must contain(appsHaveAtMost(limit)).forall
        r must contain(allAppsHaveRankingInfo(true)).forall
      }
    }
    "return an empty response if there is no ranking info for them" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns NineCardsService.right(country)
      rankingServices.getRankingForAppsWithinMoments(mockEq(usaScope), any, mockEq(moments)) returns
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
      countryServices.getCountryByIsoCode2("US") returns NineCardsService.right(country)
      rankingServices.getRankingForWidgets(mockEq(usaScope), any, mockEq(widgetMoments)) returns
        rankingForWidgetsResponse

      val response = rankingProcesses.getRankedWidgets(location, unrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedWidgetsByMoment]].which { r ⇒
        r must contain(widgetsHaveAtMost(limit)).forall
        r must contain(allWidgetsHaveRankingInfo(true)).forall
      }
    }
    "return all the widgets as ranked by using world ranking if an unknown country is given" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns NineCardsService.left(countryNotFoundError)
      rankingServices.getRankingForWidgets(mockEq(WorldScope), any, mockEq(widgetMoments)) returns
        rankingForWidgetsResponse

      val response = rankingProcesses.getRankedWidgets(location, unrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedWidgetsByMoment]].which { r ⇒
        r must contain(widgetsHaveAtMost(limit)).forall
        r must contain(allWidgetsHaveRankingInfo(true)).forall
      }
    }
    "return all the widgets as unranked if there is no ranking info for them" in new BasicScope {
      countryServices.getCountryByIsoCode2("US") returns NineCardsService.right(country)
      rankingServices.getRankingForWidgets(mockEq(usaScope), any, mockEq(widgetMoments)) returns
        rankingForWidgetsEmptyResponse

      val response = rankingProcesses.getRankedWidgets(location, unrankedAppsList, moments, limit)

      response.foldMap(testInterpreters) must beRight[List[RankedWidgetsByMoment]](Nil)
    }
  }
}
