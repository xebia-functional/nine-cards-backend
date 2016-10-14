package cards.nine.api

import akka.actor.{ Actor, ActorRefFactory }
import cats.data.Xor
import cards.nine.api.NineCardsDirectives._
import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.converters.Converters._
import cards.nine.api.messages.GooglePlayMessages._
import cards.nine.api.messages.InstallationsMessages._
import cards.nine.api.messages.PathEnumerations.PriceFilter
import cards.nine.api.messages.SharedCollectionMessages._
import cards.nine.api.messages.UserMessages._
import cards.nine.api.utils.SprayMarshallers._
import cards.nine.api.utils.SprayMatchers._
import cards.nine.commons.NineCardsService.Result
import cards.nine.processes.NineCardsServices._
import cards.nine.processes._
import cards.nine.processes.messages.ApplicationMessages.GetAppsInfoResponse
import cards.nine.services.free.domain.Category
import cards.nine.services.free.domain.rankings._
import spray.http.StatusCodes.NotFound
import spray.routing._

import scala.concurrent.ExecutionContext

class NineCardsApiActor
  extends Actor
  with AuthHeadersRejectionHandler
  with HttpService
  with NineCardsExceptionHandler {

  override val actorRefFactory = context

  implicit val executionContext: ExecutionContext = actorRefFactory.dispatcher

  def receive = runRoute(new NineCardsRoutes().nineCardsRoutes)

}

class NineCardsRoutes(
  implicit
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
    case _ ⇒ complete(NotFound)
  }

  private[this] lazy val userRoute: Route =
    pathEndOrSingleSlash {
      post {
        entity(as[ApiLoginRequest]) { request ⇒
          nineCardsDirectives.authenticateLoginRequest { sessionToken: SessionToken ⇒
            complete {
              userProcesses.signUpUser(toLoginRequest(request, sessionToken)) map toApiLoginResponse
            }
          }
        }
      }
    }

  private[this] lazy val applicationRoute: Route =
    nineCardsDirectives.authenticateUser { userContext ⇒
      path("categorize") {
        post {
          entity(as[ApiGetAppsInfoRequest]) { request ⇒
            nineCardsDirectives.googlePlayInfo { googlePlayContext ⇒
              complete(getAppsInfo(request, googlePlayContext, userContext)(toApiCategorizeAppsResponse))
            }
          }
        }
      } ~
        path("details") {
          post {
            entity(as[ApiGetAppsInfoRequest]) { request ⇒
              nineCardsDirectives.googlePlayInfo { googlePlayContext ⇒
                complete(getAppsInfo(request, googlePlayContext, userContext)(toApiDetailAppsResponse))
              }
            }
          }
        } ~
        path("rank") {
          post {
            entity(as[ApiRankAppsRequest]) { request ⇒
              complete(rankApps(request, userContext))
            }
          }
        } ~
        path("search") {
          post {
            entity(as[ApiSearchAppsRequest]) { request ⇒
              nineCardsDirectives.googlePlayInfo { googlePlayContext ⇒
                complete(searchApps(request, googlePlayContext, userContext))
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
            complete(updateInstallation(request, userContext))
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

  private type NineCardsServed[A] = cats.free.Free[NineCardsServices, A]

  private[this] def updateInstallation(
    request: ApiUpdateInstallationRequest,
    userContext: UserContext
  ): NineCardsServed[ApiUpdateInstallationResponse] =
    userProcesses
      .updateInstallation(toUpdateInstallationRequest(request, userContext))
      .map(toApiUpdateInstallationResponse)

  private[this] def getCollection(
    publicId: PublicIdentifier,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  ): NineCardsServed[XorApiGetCollectionByPublicId] =
    sharedCollectionProcesses
      .getCollectionByPublicIdentifier(
        userId           = userContext.userId.value,
        publicIdentifier = publicId.value,
        authParams       = toAuthParams(googlePlayContext, userContext)
      )
      .map(_.map(r ⇒ toApiSharedCollection(r.data)))

  private[this] def createCollection(
    request: ApiCreateCollectionRequest,
    collectionInfo: NewSharedCollectionInfo,
    userContext: UserContext
  ): NineCardsServed[ApiCreateOrUpdateCollectionResponse] =
    sharedCollectionProcesses
      .createCollection(toCreateCollectionRequest(request, collectionInfo, userContext))
      .map(toApiCreateOrUpdateCollectionResponse)

  private[this] def subscribe(
    publicId: PublicIdentifier,
    userContext: UserContext
  ): NineCardsServed[Xor[Throwable, ApiSubscribeResponse]] =
    sharedCollectionProcesses
      .subscribe(publicId.value, userContext.userId.value)
      .map(_.map(toApiSubscribeResponse))

  private[this] def updateCollection(
    publicId: PublicIdentifier,
    request: ApiUpdateCollectionRequest
  ): NineCardsServed[Xor[Throwable, ApiCreateOrUpdateCollectionResponse]] =
    sharedCollectionProcesses
      .updateCollection(publicId.value, request.collectionInfo, request.packages)
      .map(_.map(toApiCreateOrUpdateCollectionResponse))

  private[this] def unsubscribe(
    publicId: PublicIdentifier,
    userContext: UserContext
  ): NineCardsServed[Xor[Throwable, ApiUnsubscribeResponse]] =
    sharedCollectionProcesses
      .unsubscribe(publicId.value, userContext.userId.value)
      .map(_.map(toApiUnsubscribeResponse))

  private[this] def getLatestCollectionsByCategory(
    category: Category,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext,
    pageNumber: PageNumber,
    pageSize: PageSize
  ): NineCardsServed[ApiSharedCollectionList] =
    sharedCollectionProcesses
      .getLatestCollectionsByCategory(
        userId     = userContext.userId.value,
        category   = category.entryName,
        authParams = toAuthParams(googlePlayContext, userContext),
        pageNumber = pageNumber.value,
        pageSize   = pageSize.value
      )
      .map(toApiSharedCollectionList)

  private[this] def getPublishedCollections(
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  ): NineCardsServed[ApiSharedCollectionList] =
    sharedCollectionProcesses
      .getPublishedCollections(userContext.userId.value, toAuthParams(googlePlayContext, userContext))
      .map(toApiSharedCollectionList)

  private[this] def getSubscriptionsByUser(
    userContext: UserContext
  ): NineCardsServed[ApiGetSubscriptionsByUser] =
    sharedCollectionProcesses
      .getSubscriptionsByUser(userContext.userId.value)
      .map(toApiGetSubscriptionsByUser)

  private[this] def getTopCollectionsByCategory(
    category: Category,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext,
    pageNumber: PageNumber,
    pageSize: PageSize
  ): NineCardsServed[ApiSharedCollectionList] =
    sharedCollectionProcesses
      .getTopCollectionsByCategory(
        userId     = userContext.userId.value,
        category   = category.entryName,
        authParams = toAuthParams(googlePlayContext, userContext),
        pageNumber = pageNumber.value,
        pageSize   = pageSize.value
      )
      .map(toApiSharedCollectionList)

  private[this] def getAppsInfo[T](
    request: ApiGetAppsInfoRequest,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  )(converter: GetAppsInfoResponse ⇒ T): NineCardsServed[T] =
    applicationProcesses
      .getAppsInfo(request.items, toAuthParams(googlePlayContext, userContext))
      .map(converter)

  private[this] def getRecommendationsByCategory(
    request: ApiGetRecommendationsByCategoryRequest,
    category: Category,
    priceFilter: PriceFilter,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  ): NineCardsServed[ApiGetRecommendationsResponse] =
    recommendationsProcesses
      .getRecommendationsByCategory(
        category.entryName,
        priceFilter.entryName,
        request.excludePackages,
        request.limit,
        toAuthParams(googlePlayContext, userContext)
      )
      .map(toApiGetRecommendationsResponse)

  private[this] def getRecommendationsForApps(
    request: ApiGetRecommendationsForAppsRequest,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  ): NineCardsServed[ApiGetRecommendationsResponse] =
    recommendationsProcesses
      .getRecommendationsForApps(
        request.packages,
        request.excludePackages,
        request.limitPerApp.getOrElse(Int.MaxValue),
        request.limit,
        toAuthParams(googlePlayContext, userContext)
      )
      .map(toApiGetRecommendationsResponse)

  private[this] def searchApps(
    request: ApiSearchAppsRequest,
    googlePlayContext: GooglePlayContext,
    userContext: UserContext
  ): NineCardsServed[ApiSearchAppsResponse] =
    recommendationsProcesses
      .searchApps(
        request.query,
        request.excludePackages,
        request.limit,
        toAuthParams(googlePlayContext, userContext)
      )
      .map(toApiSearchAppsResponse)

  private[this] def rankApps(
    request: ApiRankAppsRequest,
    userContext: UserContext
  ): NineCardsServed[Result[ApiRankAppsResponse]] =
    rankingProcesses.getRankedDeviceApps(request.location, request.items.mapValues(toDeviceAppList))
      .map(toApiRankAppsResponse)

  private[this] object rankings {

    import cards.nine.api.converters.{ rankings ⇒ Converters }
    import cards.nine.api.messages.{ rankings ⇒ Api }
    import io.circe.spray.JsonSupport._
    import NineCardsMarshallers._

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
      val continent: Directive1[GeoScope] =
        path("continents" / ContinentSegment)
          .map(c ⇒ ContinentScope(c): GeoScope)
      val country: Directive1[GeoScope] =
        path("countries" / CountrySegment)
          .map(c ⇒ CountryScope(c): GeoScope)
      val world = path("world") & provide(WorldScope: GeoScope)

      world | continent | country
    }

    private[this] lazy val reloadParams: Directive1[RankingParams] =
      for {
        authToken ← headerValueByName(NineCardsHeaders.headerGoogleAnalyticsToken)
        apiRequest ← entity(as[Api.Reload.Request])
      } yield Converters.reload.toRankingParams(authToken, apiRequest)

    private[this] def reloadRanking(
      scope: GeoScope,
      params: RankingParams
    ): NineCardsServed[Api.Reload.XorResponse] =
      rankingProcesses.reloadRanking(scope, params).map(Converters.reload.toXorResponse)

    private[this] def getRanking(scope: GeoScope): NineCardsServed[Api.Ranking] =
      rankingProcesses.getRanking(scope).map(Converters.toApiRanking)

  }

}
