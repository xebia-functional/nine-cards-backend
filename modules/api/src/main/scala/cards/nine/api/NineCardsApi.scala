package cards.nine.api

import akka.actor.ActorRefFactory
import cards.nine.api.NineCardsDirectives._
import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.converters.Converters._
import cards.nine.api.messages.GooglePlayMessages._
import cards.nine.api.messages.InstallationsMessages._
import cards.nine.api.messages.SharedCollectionMessages._
import cards.nine.api.messages.UserMessages._
import cards.nine.api.utils.SprayMarshallers._
import cards.nine.api.utils.SprayMatchers._
import cards.nine.commons.NineCardsService.{ NineCardsService, Result }
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.domain.account.SessionToken
import cards.nine.domain.analytics._
import cards.nine.domain.application.{ BasicCard, Category, FullCard, Package, PriceFilter }
import cards.nine.domain.pagination.Page
import cards.nine.processes._
import cards.nine.processes.NineCardsServices._

import scala.concurrent.ExecutionContext
import spray.http.StatusCodes.NotFound
import spray.routing._

class NineCardsRoutes(
  implicit
  config: NineCardsConfiguration,
  userProcesses: UserProcesses[NineCardsServices],
  googleApiProcesses: GoogleApiProcesses[NineCardsServices],
  applicationProcesses: ApplicationProcesses[NineCardsServices],
  rankingProcesses: RankingProcesses[NineCardsServices],
  recommendationsProcesses: RecommendationsProcesses[NineCardsServices],
  sharedCollectionProcesses: SharedCollectionProcesses[NineCardsServices],
  refFactory: ActorRefFactory,
  executionContext: ExecutionContext
) {

  import Directives._
  import JsonFormats._

  val nineCardsRoutes: Route = pathPrefix(Segment) {
    case "apiDocs" ⇒ swaggerRoute
    case "collections" ⇒ sharedCollectionsRoute
    case "applications" ⇒ applicationRoute
    case "recommendations" ⇒ recommendationsRoute
    case "installations" ⇒ installationsRoute
    case "login" ⇒ userRoute
    case "rankings" ⇒ rankings.route
    case "widgets" ⇒ widgetRoute
    case _ ⇒ complete(NotFound)
  }

  private[this] val applicationRoute = new ApplicationRoutes().route

  private[this] lazy val userRoute: Route =
    pathEndOrSingleSlash {
      post {
        entity(as[ApiLoginRequest]) { request ⇒
          nineCardsDirectives.authenticateLoginRequest { sessionToken: SessionToken ⇒
            complete {
              userProcesses
                .signUpUser(toLoginRequest(request, sessionToken))
                .map(toApiLoginResponse)
            }
          }
        }
      }
    }

  private[this] lazy val installationsRoute: Route =
    nineCardsDirectives.authenticateUser { implicit userContext: UserContext ⇒
      pathEndOrSingleSlash {
        put {
          entity(as[ApiUpdateInstallationRequest]) { request ⇒
            complete {
              userProcesses
                .updateInstallation(toUpdateInstallationRequest(request, userContext))
                .map(toApiUpdateInstallationResponse)
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

  private[this] lazy val sharedCollectionsRoute: Route =
    nineCardsDirectives.authenticateUser { userContext: UserContext ⇒
      pathEndOrSingleSlash {
        post {
          entity(as[ApiCreateCollectionRequest]) { request ⇒
            nineCardsDirectives.generateNewCollectionInfo { collectionInfo: NewSharedCollectionInfo ⇒
              complete(createCollection(request, collectionInfo, userContext))
            }
          }
        } ~
          get {
            nineCardsDirectives.googlePlayInfo { googlePlayContext ⇒
              complete(getPublishedCollections(googlePlayContext, userContext))
            }
          }
      } ~
        (path("latest" / CategorySegment / TypedIntSegment[PageNumber] / TypedIntSegment[PageSize]) & get) {
          (category: Category, pageNumber: PageNumber, pageSize: PageSize) ⇒
            nineCardsDirectives.googlePlayInfo { googlePlayContext ⇒
              complete {
                getLatestCollectionsByCategory(
                  category          = category,
                  googlePlayContext = googlePlayContext,
                  userContext       = userContext,
                  pageNumber        = pageNumber,
                  pageSize          = pageSize
                )
              }
            }
        } ~
        (path("top" / CategorySegment / TypedIntSegment[PageNumber] / TypedIntSegment[PageSize]) & get) {
          (category: Category, pageNumber: PageNumber, pageSize: PageSize) ⇒
            nineCardsDirectives.googlePlayInfo { googlePlayContext ⇒
              complete {
                getTopCollectionsByCategory(
                  category          = category,
                  googlePlayContext = googlePlayContext,
                  userContext       = userContext,
                  pageNumber        = pageNumber,
                  pageSize          = pageSize
                )
              }
            }
        } ~
        pathPrefix("subscriptions") {
          pathEndOrSingleSlash {
            get {
              complete(getSubscriptionsByUser(userContext))
            }
          } ~
            path(TypedSegment[PublicIdentifier]) { publicIdentifier ⇒
              put(complete(subscribe(publicIdentifier, userContext))) ~
                delete(complete(unsubscribe(publicIdentifier, userContext)))
            }
        } ~
        pathPrefix(TypedSegment[PublicIdentifier]) { publicIdentifier ⇒
          pathEndOrSingleSlash {
            get {
              nineCardsDirectives.googlePlayInfo { googlePlayContext ⇒
                complete(getCollection(publicIdentifier, googlePlayContext, userContext))
              }
            } ~
              put {
                entity(as[ApiUpdateCollectionRequest]) { request ⇒
                  complete(updateCollection(publicIdentifier, request))
                }
              }
          } ~
            path("views") {
              post {
                complete(increaseViewsCountByOne(publicIdentifier))
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

  private[this] def updateInstallation(request: ApiUpdateInstallationRequest, userContext: UserContext) =
    userProcesses
      .updateInstallation(toUpdateInstallationRequest(request, userContext))
      .map(toApiUpdateInstallationResponse)

  private[this] def getCollection(
    publicId: PublicIdentifier,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  ): NineCardsService[NineCardsServices, ApiSharedCollection] =
    sharedCollectionProcesses
      .getCollectionByPublicIdentifier(
        userId           = userContext.userId.value,
        publicIdentifier = publicId.value,
        marketAuth       = toMarketAuth(googlePlayContext, userContext)
      )
      .map(r ⇒ toApiSharedCollection(r.data)(toApiCollectionApp))

  private[this] def createCollection(
    request: ApiCreateCollectionRequest,
    collectionInfo: NewSharedCollectionInfo,
    userContext: UserContext
  ): NineCardsService[NineCardsServices, ApiCreateOrUpdateCollectionResponse] =
    sharedCollectionProcesses
      .createCollection(toCreateCollectionRequest(request, collectionInfo, userContext))
      .map(toApiCreateOrUpdateCollectionResponse)

  private[this] def increaseViewsCountByOne(
    publicId: PublicIdentifier
  ): NineCardsService[NineCardsServices, ApiIncreaseViewsCountByOneResponse] =
    sharedCollectionProcesses
      .increaseViewsCountByOne(publicId.value)
      .map(toApiIncreaseViewsCountByOneResponse)

  private[this] def subscribe(
    publicId: PublicIdentifier,
    userContext: UserContext
  ): NineCardsService[NineCardsServices, ApiSubscribeResponse] =
    sharedCollectionProcesses
      .subscribe(publicId.value, userContext.userId.value)
      .map(toApiSubscribeResponse)

  private[this] def updateCollection(
    publicId: PublicIdentifier,
    request: ApiUpdateCollectionRequest
  ): NineCardsService[NineCardsServices, ApiCreateOrUpdateCollectionResponse] =
    sharedCollectionProcesses
      .updateCollection(publicId.value, request.collectionInfo, request.packages)
      .map(toApiCreateOrUpdateCollectionResponse)

  private[this] def unsubscribe(
    publicId: PublicIdentifier,
    userContext: UserContext
  ): NineCardsService[NineCardsServices, ApiUnsubscribeResponse] =
    sharedCollectionProcesses
      .unsubscribe(publicId.value, userContext.userId.value)
      .map(toApiUnsubscribeResponse)

  private[this] def getLatestCollectionsByCategory(
    category: Category,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext,
    pageNumber: PageNumber,
    pageSize: PageSize
  ): NineCardsService[NineCardsServices, ApiSharedCollectionList] =
    sharedCollectionProcesses
      .getLatestCollectionsByCategory(
        userId     = userContext.userId.value,
        category   = category.entryName,
        marketAuth = toMarketAuth(googlePlayContext, userContext),
        pageParams = Page(pageNumber.value, pageSize.value)
      )
      .map(toApiSharedCollectionList)

  private[this] def getPublishedCollections(
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  ): NineCardsService[NineCardsServices, ApiSharedCollectionList] =
    sharedCollectionProcesses
      .getPublishedCollections(userContext.userId.value, toMarketAuth(googlePlayContext, userContext))
      .map(toApiSharedCollectionList)

  private[this] def getSubscriptionsByUser(
    userContext: UserContext
  ): NineCardsService[NineCardsServices, ApiGetSubscriptionsByUser] =
    sharedCollectionProcesses
      .getSubscriptionsByUser(userContext.userId.value)
      .map(toApiGetSubscriptionsByUser)

  private[this] def getTopCollectionsByCategory(
    category: Category,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext,
    pageNumber: PageNumber,
    pageSize: PageSize
  ): NineCardsService[NineCardsServices, ApiSharedCollectionList] =
    sharedCollectionProcesses
      .getTopCollectionsByCategory(
        userId     = userContext.userId.value,
        category   = category.entryName,
        marketAuth = toMarketAuth(googlePlayContext, userContext),
        pageParams = Page(pageNumber.value, pageSize.value)
      )
      .map(toApiSharedCollectionList)

  private[this] def getRecommendationsByCategory(
    request: ApiGetRecommendationsByCategoryRequest,
    category: Category,
    priceFilter: PriceFilter,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  ): NineCardsService[NineCardsServices, ApiGetRecommendationsResponse] =
    recommendationsProcesses
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
    recommendationsProcesses
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

class ApplicationRoutes(
  implicit
  config: NineCardsConfiguration,
  userProcesses: UserProcesses[NineCardsServices],
  googleApiProcesses: GoogleApiProcesses[NineCardsServices],
  applicationProcesses: ApplicationProcesses[NineCardsServices],
  rankingProcesses: RankingProcesses[NineCardsServices],
  recommendationsProcesses: RecommendationsProcesses[NineCardsServices],
  executionContext: ExecutionContext
) {

  import Directives._
  import JsonFormats._

  lazy val route: Route = details ~ categorize ~ rank ~ search

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

  private type NineCardsServed[A] = cats.free.Free[NineCardsServices, A]

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
    recommendationsProcesses
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
  ): NineCardsServed[Result[ApiRankAppsResponse]] =
    rankingProcesses.getRankedDeviceApps(request.location, request.items)
      .map(toApiRankAppsResponse)

  private[this] def rankAppsByMoments(
    request: ApiRankByMomentsRequest,
    userContext: UserContext
  ): NineCardsServed[Result[ApiRankAppsResponse]] =
    rankingProcesses.getRankedAppsByMoment(request.location, request.items, request.moments, request.limit)
      .map(toApiRankAppsResponse)

}
