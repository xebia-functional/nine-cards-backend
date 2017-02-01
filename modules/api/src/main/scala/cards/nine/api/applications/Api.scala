/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.api.applications

import cards.nine.api.NineCardsDirectives._
import cards.nine.api.utils.SprayMarshallers._
import cards.nine.api.utils.SprayMatchers._
import cards.nine.commons.NineCardsService.{ NineCardsService, Result }
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.domain.application.{ BasicCard, Category, Package, PriceFilter }
import cards.nine.domain.market.MarketCredentials
import cards.nine.processes._
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.applications.ApplicationProcesses
import cards.nine.processes.rankings.RankingProcesses
import cards.nine.processes.NineCardsServices._
import cats.free.Free
import scala.concurrent.ExecutionContext
import spray.routing._

class ApplicationsApi(
  implicit
  config: NineCardsConfiguration,
  accountProcesses: AccountProcesses[NineCardsServices],
  applicationProcesses: ApplicationProcesses[NineCardsServices],
  rankingProcesses: RankingProcesses[NineCardsServices],
  executionContext: ExecutionContext
) {

  import Converters._
  import Directives._
  import JsonFormats._
  import messages._

  lazy val route: Route =
    pathPrefix("applications")(details ~ categorize ~ rank ~ search) ~
      pathPrefix("recommendations")(recommendationsRoute) ~
      pathPrefix("widgets")(widgetRoute)

  private[this] val details: Route =
    pathPrefix("details") {
      pathEndOrSingleSlash {
        post {
          entity(as[ApiAppsInfoRequest]) { request ⇒
            nineCardsDirectives.authenticateUser { userContext ⇒
              nineCardsDirectives.marketAuthHeaders { marketAuth ⇒
                parameter("slice".?) { sliceOpt ⇒
                  sliceOpt match {
                    case Some("icon") ⇒
                      complete(getAppsIconName(request, marketAuth))
                    case _ ⇒
                      complete(getAppsDetails(request, marketAuth))
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
            nineCardsDirectives.marketAuthHeaders { marketAuth ⇒
              complete(categorizeApps(request, marketAuth))
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
            complete(rankApps(request))
          }
        }
      } ~
        path("rank-by-moments") {
          post {
            entity(as[ApiRankByMomentsRequest]) { request ⇒
              complete(rankAppsByMoments(request))
            }
          }
        }
    }

  private[this] val search: Route =
    path("search") {
      post {
        entity(as[ApiSearchAppsRequest]) { request ⇒
          nineCardsDirectives.authenticateUser { userContext ⇒
            nineCardsDirectives.marketAuthHeaders { marketAuth ⇒
              complete(searchApps(request, marketAuth))
            }
          }
        }
      }
    }

  private[this] lazy val recommendationsRoute: Route =
    nineCardsDirectives.authenticateUser { userContext ⇒
      pathEndOrSingleSlash {
        post {
          entity(as[ApiGetRecommendationsForAppsRequest]) { request ⇒
            nineCardsDirectives.marketAuthHeaders { marketAuth ⇒
              complete(getRecommendationsForApps(request, marketAuth))
            }
          }
        }
      } ~
        pathPrefix(CategorySegment) { category ⇒
          nineCardsDirectives.priceFilterPath { priceFilter ⇒
            post {
              entity(as[ApiGetRecommendationsByCategoryRequest]) { request ⇒
                nineCardsDirectives.marketAuthHeaders { marketAuth ⇒
                  complete(getRecommendationsByCategory(request, category, priceFilter, marketAuth))
                }
              }
            }
          }
        }
    }

  private[this] lazy val widgetRoute: Route =
    nineCardsDirectives.authenticateUser { userContext ⇒
      path("rank") {
        post {
          entity(as[ApiRankByMomentsRequest]) { request ⇒
            complete(rankWidgets(request))
          }
        }
      }
    }

  private[this] def getAppsDetails(
    request: ApiAppsInfoRequest,
    marketAuth: MarketCredentials
  ): NineCardsService[NineCardsServices, ApiAppsInfoResponse[ApiDetailsApp]] =
    applicationProcesses
      .getAppsInfo(request.items, marketAuth)
      .map(toApiAppsInfoResponse(toApiDetailsApp))

  private[this] def getAppsIconName(
    request: ApiAppsInfoRequest,
    marketAuth: MarketCredentials
  ): NineCardsService[NineCardsServices, ApiAppsInfoResponse[ApiIconApp]] =
    applicationProcesses
      .getAppsBasicInfo(request.items, marketAuth)
      .map(toApiAppsInfoResponse[BasicCard, ApiIconApp](toApiIconApp))

  private[this] def categorizeApps(
    request: ApiAppsInfoRequest,
    marketAuth: MarketCredentials
  ): NineCardsService[NineCardsServices, ApiCategorizedApps] =
    applicationProcesses
      .getAppsInfo(request.items, marketAuth)
      .map(toApiCategorizedApps)

  private[this] def setAppInfo(
    packageId: Package,
    apiDetails: ApiSetAppInfoRequest
  ): NineCardsService[NineCardsServices, ApiSetAppInfoResponse] =
    applicationProcesses
      .storeCard(toFullCard(packageId, apiDetails))
      .map(toApiSetAppInfoResponse)

  private[this] def searchApps(
    request: ApiSearchAppsRequest,
    marketAuth: MarketCredentials
  ): NineCardsService[NineCardsServices, ApiSearchAppsResponse] =
    applicationProcesses
      .searchApps(
        request.query,
        request.excludePackages,
        request.limit,
        marketAuth
      )
      .map(toApiSearchAppsResponse)

  private[this] def rankApps(
    request: ApiRankAppsRequest
  ): Free[NineCardsServices, Result[ApiRankAppsResponse]] =
    rankingProcesses.getRankedDeviceApps(request.location, request.items)
      .map(toApiRankAppsResponse)

  private[this] def rankAppsByMoments(
    request: ApiRankByMomentsRequest
  ): Free[NineCardsServices, Result[ApiRankAppsResponse]] =
    rankingProcesses.getRankedAppsByMoment(request.location, request.items, request.moments, request.limit)
      .map(toApiRankAppsResponse)

  private[this] def getRecommendationsByCategory(
    request: ApiGetRecommendationsByCategoryRequest,
    category: Category,
    priceFilter: PriceFilter,
    marketAuth: MarketCredentials
  ): NineCardsService[NineCardsServices, ApiGetRecommendationsResponse] =
    applicationProcesses
      .getRecommendationsByCategory(
        category.entryName,
        priceFilter,
        request.excludePackages,
        request.limit,
        marketAuth
      )
      .map(toApiGetRecommendationsResponse)

  private[this] def getRecommendationsForApps(
    request: ApiGetRecommendationsForAppsRequest,
    marketAuth: MarketCredentials
  ): NineCardsService[NineCardsServices, ApiGetRecommendationsResponse] =
    applicationProcesses
      .getRecommendationsForApps(
        request.packages,
        request.excludePackages,
        request.limitPerApp,
        request.limit,
        marketAuth
      )
      .map(toApiGetRecommendationsResponse)

  private[this] def rankWidgets(
    request: ApiRankByMomentsRequest
  ): Free[NineCardsServices, Result[ApiRankWidgetsResponse]] =
    rankingProcesses.getRankedWidgets(request.location, request.items, request.moments, request.limit)
      .map(toApiRankWidgetsResponse)

}
