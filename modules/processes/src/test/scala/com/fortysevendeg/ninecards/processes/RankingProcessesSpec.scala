package com.fortysevendeg.ninecards.processes

import cats.data.Xor
import cats.free.Free
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import com.fortysevendeg.ninecards.processes.messages.rankings._
import com.fortysevendeg.ninecards.processes.utils.DummyNineCardsConfig
import com.fortysevendeg.ninecards.services.free.algebra.GoogleAnalytics
import com.fortysevendeg.ninecards.services.free.domain.rankings._
import com.fortysevendeg.ninecards.services.persistence.CustomComposite._
import com.fortysevendeg.ninecards.services.persistence._
import com.fortysevendeg.ninecards.services.persistence.rankings.{ Services ⇒ PersistenceServices }
import doobie.imports._
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.matcher.{ Matchers, XorMatchers }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scalaz.Scalaz._

trait RankingsProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with DummyNineCardsConfig
  with XorMatchers
  with TestInterpreters {

  import TestData.rankings._

  trait BasicScope extends Scope {

    implicit val analyticsServices: GoogleAnalytics.Services[NineCardsServices] =
      mock[GoogleAnalytics.Services[NineCardsServices]]
    implicit val persistenceServices = mock[PersistenceServices]
    implicit val rankingProcesses = new RankingProcesses[NineCardsServices]

  }

  trait SuccessfulScope extends BasicScope {

    analyticsServices.getRanking(
      scope  = mockEq(scope),
      params = mockEq(params)
    ) returns Free.pure(Xor.right(ranking))

    persistenceServices.getRanking(scope) returns ranking.point[ConnectionIO]

    //    persistenceServices.setRanking(
    //      scope   = mockEq(scope),
    //      ranking = mockEq(ranking)
    //    ) returns (0,0).point[ConnectionIO]
  }

  trait UnsuccessfulScope extends BasicScope {

    analyticsServices.getRanking(any, any) returns Free.pure(Xor.left(TestData.rankings.error))

    persistenceServices.getRanking(any) returns ranking.point[ConnectionIO]

    persistenceServices.setRanking(any, any) returns (0, 0).point[ConnectionIO]

  }

}

trait RankingsProcessesContext {

  val params = RankingParams
}

class RankingsProcessesSpec extends RankingsProcessesSpecification {

  import TestData.rankings._

  "getRanking" should {
    "give the valid ranking" in new SuccessfulScope {
      val response = rankingProcesses.getRanking(scope)
      response.foldMap(testInterpreters) mustEqual Get.Response(ranking)
    }
  }

  //  "reloadRanking" should {
  //    "give a good answer" in new SuccessfulScope {
  //      val response = rankingProcesses.reloadRanking(scope, params)
  //      response.foldMap(testInterpreters) mustEqual Reload.Response()
  //    }
  //
  //  }

}
