package cards.nine.processes

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService.Result
import cards.nine.processes.NineCardsServices._
import cards.nine.processes.TestData.Values._
import cards.nine.processes.TestData.rankings._
import cards.nine.processes.messages.rankings.GetRankedDeviceApps.RankedDeviceApp
import cards.nine.processes.messages.rankings._
import cards.nine.services.free.algebra.{ Country, GoogleAnalytics, Ranking }
import cards.nine.services.free.domain
import cards.nine.services.free.domain.rankings.UpdateRankingSummary
import cats.Id
import cats.data.Xor
import cats.free.Free
import cats.syntax.either._
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

    def hasRankingInfo(hasRanking: Boolean): Matcher[RankedDeviceApp] = {
      app: RankedDeviceApp ⇒
        app.ranking.isDefined must_== hasRanking
    }
  }

  trait SuccessfulScope extends BasicScope {

    analyticsServices.getRanking(
      scope  = any,
      params = mockEq(params)
    ) returns Free.pure(Xor.right(ranking))

    countryServices.getCountryByIsoCode2("US") returns NineCardsService.right(country)

    rankingServices.getRanking(any) returns Free.pure(ranking)

    rankingServices.updateRanking(scope, ranking) returns Free.pure(UpdateRankingSummary(0, 0))

    rankingServices.getRankingForApps(any, any) returns NineCardsService.right(rankedAppsList)
  }

  trait UnsuccessfulScope extends BasicScope {

    analyticsServices.getRanking(any, any) returns Free.pure(Xor.left(TestData.rankings.error))

    countryServices.getCountryByIsoCode2("US") returns NineCardsService.left(countryNotFoundError)

    rankingServices.getRanking(any) returns Free.pure(ranking)

    rankingServices.getRankingForApps(any, any) returns NineCardsService.right(emptyRankedAppsList)
  }

}

class RankingsProcessesSpec extends RankingsProcessesSpecification {

  import TestData.rankings._

  "getRanking" should {
    "give the valid ranking" in new SuccessfulScope {
      val response = rankingProcesses.getRanking(scope)
      response.foldMap(testInterpreters) mustEqual Get.Response(ranking)
    }
  }

  "reloadRanking" should {
    "give a good answer" in new SuccessfulScope {
      val response = rankingProcesses.reloadRanking(scope, params)
      response.foldMap(testInterpreters) mustEqual Xor.Right(Reload.Response())
    }

  }

  "getRankedDeviceApps" should {
    "return an empty response if no device apps are given" in new SuccessfulScope {
      val response = rankingProcesses.getRankedDeviceApps(location, emptyDeviceAppsMap)

      response.foldMap(testInterpreters) must beRight[Map[String, List[RankedDeviceApp]]](Map.empty[String, List[RankedDeviceApp]])
    }
    "return all the device apps as ranked if there is ranking info for them" in new SuccessfulScope {
      val response = rankingProcesses.getRankedDeviceApps(location, deviceAppsMap)

      response.foldMap(testInterpreters) must beRight[Map[String, List[RankedDeviceApp]]].which { r ⇒
        r.values.flatten must contain(hasRankingInfo(true)).forall
      }
    }
    "return all the device apps as unranked if there is no ranking info for them" in new UnsuccessfulScope {
      val response = rankingProcesses.getRankedDeviceApps(location, deviceAppsMap)

      response.foldMap(testInterpreters) must beRight[Map[String, List[RankedDeviceApp]]].which { r ⇒
        r.values.flatten must contain(hasRankingInfo(false)).forall
      }
    }

  }

}
