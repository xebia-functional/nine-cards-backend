package cards.nine.api.rankings

import akka.actor.ActorSystem
import akka.testkit._
import cards.nine.api.{ AuthHeadersRejectionHandler, NineCardsExceptionHandler }
import cards.nine.api.rankings.TestData._
import cards.nine.commons.NineCardsService
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.commons.config.NineCardsConfig
import cards.nine.processes.NineCardsServices._
import cards.nine.processes._
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.rankings.RankingProcesses
import cats.free.Free
import cats.syntax.either._
import cats.syntax.xor._
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.{ HttpRequest, StatusCodes }
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

import scala.concurrent.duration.DurationInt

trait RankingsApiSpecification
  extends Specification
  with AuthHeadersRejectionHandler
  with HttpService
  with Matchers
  with Mockito
  with NineCardsExceptionHandler
  with Specs2RouteTest {

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(20.second dilated system)

  implicit def actorRefFactory = system

  trait BasicScope extends Scope {

    implicit val accountProcesses: AccountProcesses[NineCardsServices] = mock[AccountProcesses[NineCardsServices]]

    implicit val rankingProcesses: RankingProcesses[NineCardsServices] = mock[RankingProcesses[NineCardsServices]]

    implicit val config: NineCardsConfiguration = NineCardsConfig.nineCardsConfiguration

    val routes = sealRoute(new RankingsApi().route)

  }

  trait SuccessfulScope extends BasicScope {

    rankingProcesses.getRanking(any) returns Free.pure(Either.right(Messages.getResponse))

    rankingProcesses.reloadRankingByScope(any, any) returns
      Free.pure(Either.right(Messages.reloadResponse))

    rankingProcesses.getRankedWidgets(any, any, any, any) returns
      NineCardsService.right(Messages.getRankedWidgetsResponse).value

  }

  trait FailingScope extends BasicScope {

    rankingProcesses.getRanking(any) returns
      Free.pure(Either.right(Messages.getResponse))

    rankingProcesses.reloadRankingByScope(any, any) returns
      Free.pure(Either.right(Messages.reloadResponse))

  }

}

class RankingsApiSpec extends RankingsApiSpecification {

  private[this] def internalServerError(request: HttpRequest) = {
    "return 500 Internal Server Error status code if a persistence error happens" in new FailingScope {
      request ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.InternalServerError.intValue
      }
    }.pendingUntilFixed("Pending using EitherT")
  }

  private[this] def badRequestEmptyBody(request: HttpRequest) = {
    "return a 400 BadRequest if no body is provided" in new BasicScope {
      request ~> routes ~> check {
        status.intValue shouldEqual StatusCodes.BadRequest.intValue
      }
    }
  }

  def testRanking(scopePath: String) = {
    val path = s"/rankings/$scopePath"

    s""" "GET ${path}", the endpoint to read a ranking,""" should {
      val request = Get(path)

      internalServerError(request)

      "return a 200 OK Status code if the operation was carried out" in new SuccessfulScope {
        request ~> routes ~> check {
          status.intValue shouldEqual StatusCodes.OK.intValue
        }
      }

    }

    s""" "POST $path", the endpoint to refresh an ranking,""" should {

      import NineCardsMarshallers._

      val request = Post(path, Messages.reloadApiRequest)

      "return a 200 OK Status code if the operation was carried out" in new SuccessfulScope {
        request ~> addHeaders(Headers.googleAnalyticsHeaders) ~> routes ~> check {
          status.intValue shouldEqual StatusCodes.OK.intValue
        }
      }
    }

  }

  val rankingPaths: List[String] = {
    val countries = List("countries/es", "countries/ES", "countries/gb", "countries/us", "countries/it")
    "world" :: countries
  }

  rankingPaths foreach testRanking

}
