package cards.nine.api.collections

import akka.actor.ActorRefFactory
import cards.nine.api.NineCardsDirectives._
import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.collections.messages._
import cards.nine.api.utils.SprayMarshallers._
import cards.nine.api.utils.SprayMatchers._
import cards.nine.commons.NineCardsService.NineCardsService
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.domain.application.Category
import cards.nine.domain.pagination.Page
import cards.nine.processes._
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.collections.SharedCollectionProcesses
import cards.nine.processes.NineCardsServices._

import scala.concurrent.ExecutionContext
import spray.routing._

class CollectionsApi(
  implicit
  config: NineCardsConfiguration,
  accountProcesses: AccountProcesses[NineCardsServices],
  sharedCollectionProcesses: SharedCollectionProcesses[NineCardsServices],
  refFactory: ActorRefFactory,
  executionContext: ExecutionContext
) {

  import Converters._
  import Directives._
  import JsonFormats._

  val route: Route =
    pathPrefix("collections") {
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

    }

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

}
