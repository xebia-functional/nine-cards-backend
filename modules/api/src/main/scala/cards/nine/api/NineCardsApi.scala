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
import spray.http.StatusCodes.SeeOther
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
      loaderIoRoute ~
      websiteRoutes ~
      (new AccountsApi().route) ~
      (new ApplicationsApi().route) ~
      (new CollectionsApi().route) ~
      (new RankingsApi().route)

  private[this] lazy val loaderIoRoute = {
    val loaderIoToken = config.loaderIO.verificationToken
    val loaderIoFile = s"loaderio-${loaderIoToken}.txt"

    pathPrefix(loaderIoFile)(complete(s"loaderio-${loaderIoToken}"))
  }

  private[this] lazy val healthcheckRoute: Route =
    path("healthcheck")(complete("Nine Cards Backend Server Health Check"))

  private[this] lazy val websiteRoutes: Route =
    pathEndOrSingleSlash {
      redirect(config.webmainpage, SeeOther)
    } ~
      pathPrefix("shared-collection" / TypedSegment[PublicIdentifier]) { _publicId ⇒
        getFromResource("web/collection.html")
      } ~
      path("tos") {
        getFromResource("web/tos.html")
      } ~
      pathPrefix("web") {
        getFromResourceDirectory("web")
      }

}

