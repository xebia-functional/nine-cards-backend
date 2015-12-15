package com.fortysevendeg.ninecards.api

import akka.actor.Actor
import com.fortysevendeg.ninecards.processes.{UserProcesses, AppProcesses}
import com.fortysevendeg.ninecards.processes.messages._
import com.fortysevendeg.ninecards.services.free.domain.User
import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.processes.domain._
import spray.httpx.SprayJsonSupport
import spray.routing._
import scala.language.{higherKinds, implicitConversions}
import scalaz.concurrent.Task
import FreeUtils._
import NineCardsApiHeaderCommons._

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
      installationsApiRoute ~
      appsApiRoute() ~
      swaggerApiRoute

  private[this] def userApiRoute()(implicit userProcesses: UserProcesses[NineCardsServices]) =
    pathPrefix("users") {
      pathEndOrSingleSlash {
        requestLoginHeaders {
          (appId, apiKey) =>
            post {
              entity(as[AddUserRequest]){
                request =>
                  //                  complete(Map("result" -> s"Gets user info: ${request.authData.google.email}"))
                  complete{
                    val result: Task[User] = userProcesses.addUser(request)
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
                  complete(
                    Map("result" -> s"Updates user info: $userId")
                  )
                }
          }
        } ~
        path("link") {
          requestFullHeaders {
            (appId, apiKey, sessionToken, androidId, localization) =>
              put {
                complete(
                  Map("result" -> s"Links new account with specific user")
                )
              }
          }
        }
    }

  private[this] def installationsApiRoute =
    path("installations") {
      post {
        complete(
          Map("result" -> s"Creates new installation")
        )
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