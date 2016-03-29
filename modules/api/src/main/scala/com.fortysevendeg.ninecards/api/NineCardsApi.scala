package com.fortysevendeg.ninecards.api

import akka.actor.Actor
import com.fortysevendeg.ninecards.api.NineCardsApiHeaderCommons._
import com.fortysevendeg.ninecards.api.NineCardsAuthenticator._
import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain.UserContext
import com.fortysevendeg.ninecards.api.converters.Converters._
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.UserMessages._
import com.fortysevendeg.ninecards.api.utils.FreeUtils._
import com.fortysevendeg.ninecards.api.utils.TaskUtils._
import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.processes.{GoogleApiProcesses, UserProcesses}
import spray.httpx.SprayJsonSupport
import spray.routing._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class NineCardsApiActor
  extends Actor
    with NineCardsApi
    with AuthHeadersRejectionHandler
    with NineCardsExceptionHandler {

  def actorRefFactory = context

  implicit def executionContext: ExecutionContext = actorRefFactory.dispatcher

  def receive = runRoute(nineCardsApiRoute)

}

trait NineCardsApi
  extends HttpService
    with SprayJsonSupport
    with JsonFormats {

  def nineCardsApiRoute[T](
    implicit userProcesses: UserProcesses[NineCardsServices],
    googleApiProcesses: GoogleApiProcesses[NineCardsServices],
    executionContext: ExecutionContext): Route =
    userApiRoute ~
      installationsApiRoute ~
      swaggerApiRoute

  private[this] def userApiRoute(
    implicit userProcesses: UserProcesses[NineCardsServices],
    googleApiProcesses: GoogleApiProcesses[NineCardsServices],
    executionContext: ExecutionContext) =
    pathPrefix("login") {
      pathEndOrSingleSlash {
        requestLoginHeaders { (appId, apiKey) =>
          nineCardsAuthenticator.authenticateLoginRequest {
            post {
              entity(as[ApiLoginRequest]) { request =>
                complete {
                  userProcesses.signUpUser(request) map toApiLoginResponse
                }
              }
            }
          }
        }
      }
    }

  private[this] def installationsApiRoute(
    implicit userProcesses: UserProcesses[NineCardsServices],
    executionContext: ExecutionContext) =
    pathPrefix("installations") {
      pathEndOrSingleSlash {
        nineCardsAuthenticator.authenticateUser { implicit userContext: UserContext =>
          put {
            entity(as[ApiUpdateInstallationRequest]) { request =>
              complete {
                userProcesses.updateInstallation(request) map toApiUpdateInstallationResponse
              }
            }
          }
        }
      }
    }

  private[this] def swaggerApiRoute =
  // This path prefix grants access to the Swagger documentation.
  // Both /apiDocs/ and /apiDocs/index.html are valid paths to load Swagger-UI.
    pathPrefix("apiDocs") {
      pathEndOrSingleSlash {
        getFromResource("apiDocs/index.html")
      } ~ {
        getFromResourceDirectory("apiDocs")
      }
    }
}