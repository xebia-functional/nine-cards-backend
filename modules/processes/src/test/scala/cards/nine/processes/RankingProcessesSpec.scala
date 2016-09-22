package cards.nine.processes

import cards.nine.processes.NineCardsServices._
import cards.nine.processes.messages.rankings.GetRankedDeviceApps.RankedDeviceApp
import cards.nine.processes.messages.rankings._
import cards.nine.processes.utils.DatabaseContext._
import cards.nine.services.free.algebra.GoogleAnalytics
import cards.nine.services.persistence.rankings.{ Services ⇒ PersistenceServices }
import cats.data.Xor
import cats.free.Free
import doobie.imports._
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.matcher.{ Matcher, Matchers, XorMatchers }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.Scalaz._

trait RankingsProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with XorMatchers
  with TestInterpreters {

  import TestData.rankings._

  trait BasicScope extends Scope {

    implicit val analyticsServices: GoogleAnalytics.Services[NineCardsServices] =
      mock[GoogleAnalytics.Services[NineCardsServices]]
    implicit val persistenceServices = mock[PersistenceServices]

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

    persistenceServices.getRanking(any) returns ranking.point[ConnectionIO]

    persistenceServices.setRanking(scope, ranking) returns (0, 0).point[ConnectionIO]

    persistenceServices.getRankedApps(any, any) returns rankedAppsList.point[ConnectionIO]
  }

  trait UnsuccessfulScope extends BasicScope {

    analyticsServices.getRanking(any, any) returns Free.pure(Xor.left(TestData.rankings.error))

    persistenceServices.getRanking(any) returns ranking.point[ConnectionIO]

    persistenceServices.getRankedApps(any, any) returns emptyRankedAppsList.point[ConnectionIO]
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
      val response = rankingProcesses.getRankedDeviceApps(scope, emptyDeviceAppsMap)

      response.foldMap(testInterpreters) must beEmpty
    }
    "return all the device apps as ranked if there is ranking info for them" in new SuccessfulScope {
      val response = rankingProcesses.getRankedDeviceApps(scope, deviceAppsMap)

      response.foldMap(testInterpreters).values.flatten must contain(hasRankingInfo(true)).forall
    }
    "return all the device apps as unranked if there is no ranking info for them" in new UnsuccessfulScope {
      val response = rankingProcesses.getRankedDeviceApps(scope, deviceAppsMap)

      response.foldMap(testInterpreters).values.flatten must contain(hasRankingInfo(false)).forall
    }

  }

}
