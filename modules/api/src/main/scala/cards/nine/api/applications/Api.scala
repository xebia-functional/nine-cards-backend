package cards.nine.api.applications

import cards.nine.api.NineCardsDirectives._
import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.converters.Converters._
import cards.nine.api.messages.GooglePlayMessages._
import cards.nine.api.utils.SprayMarshallers._
import cards.nine.api.utils.SprayMatchers._
import cards.nine.commons.NineCardsService.{ NineCardsService, Result }
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.domain.application.{ BasicCard, FullCard, Package }
import cards.nine.processes._
import cards.nine.processes.applications.ApplicationProcesses
import cards.nine.processes.NineCardsServices._
import scala.concurrent.ExecutionContext
import spray.routing._

class ApplicationsApi(
  implicit
  config: NineCardsConfiguration,
  userProcesses: UserProcesses[NineCardsServices],
  applicationProcesses: ApplicationProcesses[NineCardsServices],
  rankingProcesses: RankingProcesses[NineCardsServices],
  executionContext: ExecutionContext
) {

  import Converters._
  import Directives._
  import JsonFormats._
  import messages._

  lazy val route: Route = pathPrefix("applications")(details ~ categorize ~ rank ~ search)

  private[this] val details: Route =
    pathPrefix("details") {
      pathEndOrSingleSlash {
        post {
          entity(as[ApiAppsInfoRequest]) { request ⇒
            nineCardsDirectives.authenticateUser { userContext ⇒
              nineCardsDirectives.googlePlayInfo { googlePlayContext ⇒
                parameter("slice".?) { sliceOpt ⇒
                  sliceOpt match {
                    case Some("icon") ⇒
                      complete(getAppsBasicInfo(request, googlePlayContext, userContext)(toApiIconApp))
                    case _ ⇒
                      complete(getAppsInfo(request, googlePlayContext, userContext)(toApiDetailsApp))
                  }
                }
              }
            }
          }
        }
      } ~
        path(PackageSegment) { packageId ⇒
          put {
            authenticate(nineCardsDirectives.editorAuth) { userName ⇒
              entity(as[ApiSetAppInfoRequest]) { details ⇒
                complete(setAppInfo(packageId, details))
              }
            }
          }
        }
    }

  private[this] val categorize: Route =
    nineCardsDirectives.authenticateUser { userContext ⇒
      path("categorize") {
        post {
          entity(as[ApiAppsInfoRequest]) { request ⇒
            nineCardsDirectives.googlePlayInfo { googlePlayContext ⇒
              complete(getAppsInfo(request, googlePlayContext, userContext)(toApiCategorizedApp))
            }
          }
        }
      }
    }

  private[this] val rank: Route =
    nineCardsDirectives.authenticateUser { userContext ⇒
      path("rank") {
        post {
          entity(as[ApiRankAppsRequest]) { request ⇒
            complete(rankApps(request, userContext))
          }
        }
      } ~
        path("rank-by-moments") {
          post {
            entity(as[ApiRankByMomentsRequest]) { request ⇒
              complete(rankAppsByMoments(request, userContext))
            }
          }
        }
    }

  private[this] val search: Route =
    path("search") {
      post {
        entity(as[ApiSearchAppsRequest]) { request ⇒
          nineCardsDirectives.authenticateUser { userContext ⇒
            nineCardsDirectives.googlePlayInfo { googlePlayContext ⇒
              complete(searchApps(request, googlePlayContext, userContext))
            }
          }
        }
      }
    }

  private[this] def getAppsInfo[T](
    request: ApiAppsInfoRequest,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  )(converter: FullCard ⇒ T): NineCardsService[NineCardsServices, ApiAppsInfoResponse[T]] =
    applicationProcesses
      .getAppsInfo(request.items, toMarketAuth(googlePlayContext, userContext))
      .map(toApiAppsInfoResponse(converter))

  private[this] def getAppsBasicInfo[T](
    request: ApiAppsInfoRequest,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  )(converter: BasicCard ⇒ T): NineCardsService[NineCardsServices, ApiAppsInfoResponse[T]] =
    applicationProcesses
      .getAppsBasicInfo(request.items, toMarketAuth(googlePlayContext, userContext))
      .map(toApiAppsInfoResponse[BasicCard, T](converter))

  private[this] def setAppInfo(
    packageId: Package,
    apiDetails: ApiSetAppInfoRequest
  ): NineCardsService[NineCardsServices, ApiSetAppInfoResponse] =
    applicationProcesses
      .storeCard(toFullCard(packageId, apiDetails))
      .map(toApiSetAppInfoResponse)

  private[this] def searchApps(
    request: ApiSearchAppsRequest,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  ): NineCardsService[NineCardsServices, ApiSearchAppsResponse] =
    applicationProcesses
      .searchApps(
        request.query,
        request.excludePackages,
        request.limit,
        toMarketAuth(googlePlayContext, userContext)
      )
      .map(toApiSearchAppsResponse)

  private[this] def rankApps(
    request: ApiRankAppsRequest,
    userContext: UserContext
  ): cats.free.Free[NineCardsServices, Result[ApiRankAppsResponse]] =
    rankingProcesses.getRankedDeviceApps(request.location, request.items)
      .map(toApiRankAppsResponse)

  private[this] def rankAppsByMoments(
    request: ApiRankByMomentsRequest,
    userContext: UserContext
  ): cats.free.Free[NineCardsServices, Result[ApiRankAppsResponse]] =
    rankingProcesses.getRankedAppsByMoment(request.location, request.items, request.moments, request.limit)
      .map(toApiRankAppsResponse)

}
