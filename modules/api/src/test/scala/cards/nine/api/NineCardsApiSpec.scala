package cards.nine.api

import akka.actor.ActorSystem
import akka.testkit._
import cards.nine.api.TestData._
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
import org.specs2.specification.Scope
import scala.concurrent.duration.DurationInt
import spray.http.{ MediaTypes, StatusCodes }
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

trait NineCardsApiSpecification
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

  trait BasicScope extends Scope {

    implicit val accountProcesses: AccountProcesses[NineCardsServices] = mock[AccountProcesses[NineCardsServices]]

    implicit val applicationProcesses: ApplicationProcesses[NineCardsServices] = mock[ApplicationProcesses[NineCardsServices]]

    implicit val rankingProcesses: RankingProcesses[NineCardsServices] = mock[RankingProcesses[NineCardsServices]]

    implicit val config: NineCardsConfiguration = NineCardsConfig.nineCardsConfiguration

    val nineCardsApi = new NineCardsRoutes().nineCardsRoutes

  }

}

class NineCardsApiSpec
  extends NineCardsApiSpecification {

  "nineCardsApi" should {
    "grant access to Swagger documentation" in new BasicScope {
      Get(Paths.apiDocs) ~>
        sealRoute(nineCardsApi) ~>
        check {
          status should be equalTo StatusCodes.OK.intValue
          mediaType === MediaTypes.`text/html`
          responseAs[String] must contain("Swagger")
        }
    }

    "return a 404 NotFound error for an undefined path " in new BasicScope {
      Get(Paths.invalid) ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual StatusCodes.NotFound.intValue
      }
    }

  }

}
