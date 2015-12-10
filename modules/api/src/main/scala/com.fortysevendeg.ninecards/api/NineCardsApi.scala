package com.fortysevendeg.ninecards.api

import akka.actor.Actor
import cats.free.Free
import com.fortysevendeg.ninecards.processes.AppProcesses
import com.fortysevendeg.ninecards.processes.domain._
import spray.httpx.SprayJsonSupport
import spray.routing._

import scala.language.{higherKinds, implicitConversions}
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

  import FreeUtils._
  import NineCardsApiHeaderCommons._

  def nineCardsApiRoute(implicit AP: AppProcesses) = {

    import AP._

    pathPrefix("users") {
      pathEndOrSingleSlash {
        post {
          complete("Starts a session")
        }
      } ~
        path(Segment) { userId =>
          requestLoginHeaders {
            (appId, apiKey) =>
              get {
                complete {
                  val result: Task[User] = userbyIdUser(userId)
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
    } ~
      path("installations") {
        post {
          complete(
            Map("result" -> s"Creates new installation")
          )
        }
      } ~
      pathPrefix("apps") {
        path("categorize") {
          get {
            complete {
              val result: Task[Seq[GooglePlayApp]] = categorizeApps(Seq("com.fortysevendeg.ninecards"))

              result
            }
          }
        }
      } ~
      // This path prefix grants access to the Swagger documentation.
      // Both /apiDocs/ and /apiDocs/index.html are valid paths to load Swagger-UI.
      pathPrefix("apiDocs") {
        path("") {
          getFromResource("apiDocs/index.html")
        } ~ {
          getFromResourceDirectory("apiDocs")
        }
      }
  } ~
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
