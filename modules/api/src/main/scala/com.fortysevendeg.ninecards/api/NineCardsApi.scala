package com.fortysevendeg.ninecards.api

import akka.actor.Actor
import com.fortysevendeg.ninecards.processes.messages._
import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.processes.{AppProcesses, UserProcesses}
import com.fortysevendeg.ninecards.processes.domain._
import spray.httpx.SprayJsonSupport
import spray.routing._
import scala.language.{higherKinds, implicitConversions}
import FreeUtils._
import NineCardsApiHeaderCommons._
import scalaz.concurrent.Task

class NineCardsApiActor
  extends Actor
  with NineCardsApi
  with AuthHeadersRejectionHandler {

  def actorRefFactory = context

  def receive = runRoute(nineCardsApiRoute)

}

trait NineCardsApi
  extends HttpService
  with SprayJsonSupport
  with JsonFormats {

  def nineCardsApiRoute(implicit appProcesses: AppProcesses[NineCardsServices], userProcesses: UserProcesses[NineCardsServices]): Route =
    userApiRoute() ~
      installationsApiRoute() ~
      appsApiRoute() ~
      swaggerApiRoute

  private[this] def userApiRoute()(implicit userProcesses: UserProcesses[NineCardsServices]) =
    pathPrefix("users") {
      pathEndOrSingleSlash {
        requestLoginHeaders {
          (appId, apiKey) =>
            post {
              entity(as[AddUserRequest]) {
                request =>
                  complete {
                    val result: Task[User] = userProcesses.signUpUser(request)
                    result
                  }
              }
            }
        }
      } ~
        path(Segment) { userId =>
          requestLoginHeaders {
            (appId, apiKey) =>
              get {
                complete {
                  val result: Task[User] = userProcesses.getUserById(userId)
                  result
                }
              } ~
                put {
                  entity(as[AddUserRequest]) {
                    request =>
                      complete {
                        val result: Task[User] = userProcesses.updateUser(userId, request)
                        result
                      }
                  }
                }
          }
        }
    }

  private[this] def installationsApiRoute()(implicit userProcesses: UserProcesses[NineCardsServices]) =
    path("installations") {
      pathEndOrSingleSlash {
        requestLoginHeaders {
          (appId, apiKey) =>
            post {
              entity(as[InstallationRequest]) {
                request =>
                  complete {
                    val result: Task[Installation] = userProcesses.createInstallation(request)
                    result
                  }
              }
            }
        }
      }
    }

  private[this] def appsApiRoute()(implicit appProcesses: AppProcesses[NineCardsServices]) =
    pathPrefix("apps") {
      path("categorize") {
        get {
          complete {
            val result: Task[Seq[GooglePlayApp]] = appProcesses.categorizeApps(Seq("com.fortysevendeg.ninecards"))
            result
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