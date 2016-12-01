package cards.nine.api

import akka.actor.ActorRefFactory
import cards.nine.api.NineCardsDirectives._
import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.applications.ApplicationsApi
import cards.nine.api.collections.CollectionsApi
import cards.nine.api.converters.Converters._
import cards.nine.api.messages.GooglePlayMessages._
import cards.nine.api.accounts.AccountsApi
import cards.nine.api.utils.SprayMarshallers._
import cards.nine.api.utils.SprayMatchers._
import cards.nine.commons.NineCardsService.{ NineCardsService, Result }
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.domain.analytics._
import cards.nine.domain.application.{ Category, PriceFilter }
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
  import JsonFormats._

  lazy val nineCardsRoutes: Route = {
    val accountsRoute = new AccountsApi().route
    val applicationRoute = new ApplicationsApi().route
    val collectionsRoute = new CollectionsApi().route

    pathPrefix("apiDocs")(swaggerRoute) ~
      accountsRoute ~
      applicationRoute ~
      collectionsRoute ~
      pathPrefix("rankings")(rankings.route) ~
      pathPrefix("recommendations")(recommendationsRoute) ~
      pathPrefix("widgets")(widgetRoute)
  }

  private[this] lazy val recommendationsRoute: Route =
    nineCardsDirectives.authenticateUser { userContext ⇒
      pathEndOrSingleSlash {
        post {
          entity(as[ApiGetRecommendationsForAppsRequest]) { request ⇒
            nineCardsDirectives.googlePlayInfo { googlePlayContext ⇒
              complete(getRecommendationsForApps(request, googlePlayContext, userContext))
            }
          }
        }
      } ~
        pathPrefix(CategorySegment) { category ⇒
          nineCardsDirectives.priceFilterPath { priceFilter ⇒
            post {
              entity(as[ApiGetRecommendationsByCategoryRequest]) { request ⇒
                nineCardsDirectives.googlePlayInfo { googlePlayContext ⇒
                  complete(getRecommendationsByCategory(request, category, priceFilter, googlePlayContext, userContext))
                }
              }
            }
          }
        }
    }

  private[this] lazy val swaggerRoute: Route =
    // This path prefix grants access to the Swagger documentation.
    // Both /apiDocs/ and /apiDocs/index.html are valid paths to load Swagger-UI.
    pathEndOrSingleSlash {
      getFromResource("apiDocs/index.html")
    } ~ {
      getFromResourceDirectory("apiDocs")
    }

  private[this] lazy val widgetRoute: Route =
    nineCardsDirectives.authenticateUser { userContext ⇒
      path("rank") {
        post {
          entity(as[ApiRankByMomentsRequest]) { request ⇒
            complete(rankWidgets(request, userContext))
          }
        }
      }
    }

  private type NineCardsServed[A] = cats.free.Free[NineCardsServices, A]

  private[this] def getRecommendationsByCategory(
    request: ApiGetRecommendationsByCategoryRequest,
    category: Category,
    priceFilter: PriceFilter,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  ): NineCardsService[NineCardsServices, ApiGetRecommendationsResponse] =
    applicationProcesses
      .getRecommendationsByCategory(
        category.entryName,
        priceFilter,
        request.excludePackages,
        request.limit,
        toMarketAuth(googlePlayContext, userContext)
      )
      .map(toApiGetRecommendationsResponse)

  private[this] def getRecommendationsForApps(
    request: ApiGetRecommendationsForAppsRequest,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  ): NineCardsService[NineCardsServices, ApiGetRecommendationsResponse] =
    applicationProcesses
      .getRecommendationsForApps(
        request.packages,
        request.excludePackages,
        request.limitPerApp.getOrElse(Int.MaxValue),
        request.limit,
        toMarketAuth(googlePlayContext, userContext)
      )
      .map(toApiGetRecommendationsResponse)

  private[this] def rankWidgets(
    request: ApiRankByMomentsRequest,
    userContext: UserContext
  ): NineCardsServed[Result[ApiRankWidgetsResponse]] =
    rankingProcesses.getRankedWidgets(request.location, request.items, request.moments, request.limit)
      .map(toApiRankWidgetsResponse)

  private[this] object rankings {

    import NineCardsMarshallers._
    import cards.nine.api.converters.{ rankings ⇒ Converters }
    import cards.nine.api.messages.{ rankings ⇒ Api }

    lazy val route: Route =
      geographicScope { scope ⇒
        get {
          complete(getRanking(scope))
        } ~
          post {
            reloadParams(params ⇒ complete(reloadRanking(scope, params)))
          }
      }

    private[this] lazy val geographicScope: Directive1[GeoScope] = {
      val country: Directive1[GeoScope] =
        path("countries" / TypedSegment[CountryIsoCode])
          .map(c ⇒ CountryScope(c): GeoScope)
      val world = path("world") & provide(WorldScope: GeoScope)

      world | country
    }

    private[this] lazy val reloadParams: Directive1[RankingParams] =
      for {
        authToken ← headerValueByName(NineCardsHeaders.headerGoogleAnalyticsToken)
        apiRequest ← entity(as[Api.Reload.Request])
      } yield Converters.reload.toRankingParams(authToken, apiRequest)

    private[this] def reloadRanking(
      scope: GeoScope,
      params: RankingParams
    ): NineCardsServed[Result[Api.Reload.Response]] =
      rankingProcesses.reloadRankingByScope(scope, params).map(Converters.reload.toApiResponse)

    private[this] def getRanking(scope: GeoScope): NineCardsServed[Result[Api.Ranking]] =
      rankingProcesses.getRanking(scope).map(Converters.toApiRanking)

  }

}
