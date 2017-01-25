package cards.nine.api

import akka.actor.ActorSystem
import akka.testkit._
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.commons.config.NineCardsConfig
import cards.nine.processes.NineCardsServices._
import cards.nine.processes._
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.applications.ApplicationProcesses
import cards.nine.processes.rankings.RankingProcesses
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import scala.concurrent.duration.DurationInt
import spray.http.StatusCodes.{ OK, NotFound }
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

class NineCardsApiSpec
  extends Specification
  with AuthHeadersRejectionHandler
  with HttpService
  with JsonFormats
  with Matchers
  with Mockito
  with NineCardsExceptionHandler
  with Specs2RouteTest {

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(20.second dilated system)

  implicit def actorRefFactory = system

  implicit val accountProcesses: AccountProcesses[NineCardsServices] = mock[AccountProcesses[NineCardsServices]]

  implicit val applicationProcesses: ApplicationProcesses[NineCardsServices] = mock[ApplicationProcesses[NineCardsServices]]

  implicit val rankingProcesses: RankingProcesses[NineCardsServices] = mock[RankingProcesses[NineCardsServices]]

  implicit val config: NineCardsConfiguration = NineCardsConfig.nineCardsConfiguration

  val route = sealRoute(new NineCardsRoutes().nineCardsRoutes)

  "nineCardsApi" should {
    "grant access to HealthCheck" in {
      Get("/healthcheck") ~> route ~> check {
        status should be equalTo OK.intValue
      }
    }

    "return a 404 NotFound error for an undefined path " in {
      Get("/chalkyTown") ~> route ~> check {
        status.intValue shouldEqual NotFound.intValue
      }
    }

  }

}
