package com.fortysevendeg.ninecards.api

import akka.actor.{ Actor, ActorRefFactory }
import com.fortysevendeg.ninecards.api.NineCardsApiHeaderCommons._
import com.fortysevendeg.ninecards.api.NineCardsAuthenticator._
import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain._
import com.fortysevendeg.ninecards.api.converters.Converters._
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.api.messages.UserMessages._
import com.fortysevendeg.ninecards.api.utils.SprayMarshallers._
import com.fortysevendeg.ninecards.api.utils.SprayMatchers._
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import com.fortysevendeg.ninecards.processes._
import spray.httpx.SprayJsonSupport
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
  sharedCollectionProcesses: SharedCollectionProcesses[NineCardsServices],
  refFactory: ActorRefFactory,
  executionContext: ExecutionContext
) {

  import Directives._
  import JsonFormats._

  val nineCardsRoutes: Route = pathPrefix(Segment) {
    case "apiDocs" ⇒ swaggerRoute
    case "collections" ⇒ sharedCollectionsRoute
    case "installations" ⇒ installationsRoute
    case "login" ⇒ userRoute
  }

  private[this] lazy val userRoute =
    pathEndOrSingleSlash {
      requestLoginHeaders { (appId, apiKey) ⇒
        nineCardsAuthenticator.authenticateLoginRequest {
          post {
            entity(as[ApiLoginRequest]) { request ⇒
              complete {
                userProcesses.signUpUser(toLoginRequest(request)) map toApiLoginResponse
              }
            }
          }
        }
      }
    }

  private[this] lazy val installationsRoute =
    nineCardsAuthenticator.authenticateUser { userContext: UserContext ⇒
      pathEndOrSingleSlash {
        put {
          entity(as[ApiUpdateInstallationRequest]) { request ⇒
            complete(updateInstallation(request, userContext))
          }
        }
      }
    }

  private[this] lazy val sharedCollectionsRoute =
    nineCardsAuthenticator.authenticateUser { userContext: UserContext ⇒
      pathEndOrSingleSlash {
        get(complete(getPublishedCollections(userContext))) ~
          post {
            entity(as[ApiCreateCollectionRequest]) { request ⇒
              complete(createCollection(request, userContext))
            }
          }
      } ~
        pathPrefix(TypedSegment[PublicIdentifier]) { publicIdentifier ⇒
          pathEndOrSingleSlash {
            get(complete(getCollection(publicIdentifier)))
          } ~
            path("subscribe") {
              put(complete(subscribe(publicIdentifier, userContext))) ~
                delete(complete(unsubscribe(publicIdentifier, userContext)))
            }
        }
    }

  private[this] lazy val swaggerRoute =
    // This path prefix grants access to the Swagger documentation.
    // Both /apiDocs/ and /apiDocs/index.html are valid paths to load Swagger-UI.
    pathEndOrSingleSlash {
      getFromResource("apiDocs/index.html")
    } ~ {
      getFromResourceDirectory("apiDocs")
    }

  private[this] def updateInstallation(request: ApiUpdateInstallationRequest, userContext: UserContext) =
    userProcesses
      .updateInstallation(toUpdateInstallationRequest(request, userContext))
      .map(toApiUpdateInstallationResponse)

  private[this] def getCollection(publicId: PublicIdentifier) =
    sharedCollectionProcesses
      .getCollectionByPublicIdentifier(publicId.value)
      .map(toXorApiSharedCollection)

  private[this] def createCollection(request: ApiCreateCollectionRequest, userContext: UserContext) =
    sharedCollectionProcesses
      .createCollection(toCreateCollectionRequest(request, userContext))
      .map(toApiCreateCollectionResponse)

  private[this] def subscribe(publicId: PublicIdentifier, userContext: UserContext) =
    sharedCollectionProcesses
      .subscribe(publicId.value, userContext.userId.value)
      .map(_.map(toApiSubscribeResponse))

  private[this] def unsubscribe(publicId: PublicIdentifier, userContext: UserContext) =
    sharedCollectionProcesses
      .unsubscribe(publicId.value, userContext.userId.value)
      .map(_.map(toApiUnsubscribeResponse))

  private[this] def getPublishedCollections(userContext: UserContext) =
    sharedCollectionProcesses
      .getPublishedCollections(userContext.userId.value)
      .map(_.collections.map(toApiSharedCollection))
}
