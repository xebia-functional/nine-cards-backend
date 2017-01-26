package cards.nine.api

import akka.actor.ActorRefFactory
import cards.nine.api.NineCardsHeaders.Domain.PublicIdentifier
import cards.nine.api.applications.ApplicationsApi
import cards.nine.api.collections.CollectionsApi
import cards.nine.api.rankings.RankingsApi
import cards.nine.api.accounts.AccountsApi
import cards.nine.api.utils.SprayMatchers.TypedSegment
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.processes._
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.applications.ApplicationProcesses
import cards.nine.processes.rankings.RankingProcesses
import cards.nine.processes.NineCardsServices._

import scala.concurrent.ExecutionContext
import spray.routing._

class NineCardsRoutes(
  implicit
  config: NineCardsConfiguration,
  accountProcesses: AccountProcesses[NineCardsServices],
  applicationProcesses: ApplicationProcesses[NineCardsServices],
  rankingProcesses: RankingProcesses[NineCardsServices],
  refFactory: ActorRefFactory,
  executionContext: ExecutionContext
) {

  import Directives._

  lazy val nineCardsRoutes: Route =
    healthcheckRoute ~
      pathPrefix("web")(webRoute) ~
      pathPrefix("tos")(tosRoute) ~
      pathPrefix("shared-collection" / TypedSegment[PublicIdentifier]) {
        _ ⇒ sharedCollectionRoute
      } ~
      (new AccountsApi().route) ~
      (new ApplicationsApi().route) ~
      (new CollectionsApi().route) ~
      (new RankingsApi().route)

  private[this] lazy val healthcheckRoute: Route =
    path("healthcheck")(complete("Nine Cards Backend Server Health Check"))

  private[this] lazy val tosRoute: Route =
    pathEnd {
      getFromResource("web/tos.html")
    }

  private[this] lazy val webRoute: Route = getFromResourceDirectory("web")

  private[this] lazy val sharedCollectionRoute: Route =
    getFromResource("web/collection.html")

}
