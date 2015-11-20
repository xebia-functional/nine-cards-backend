package com.fortysevendeg.ninecards.api

import akka.actor.Actor
import com.fortysevendeg.ninecards.processes.domain.GooglePlayApp
import spray.httpx.SprayJsonSupport
import spray.routing._

import scala.language.{higherKinds, implicitConversions}
import scalaz.Id

class NineCardsApiActor extends Actor with NineCardsApi {

  def actorRefFactory = context

  def receive = runRoute(nineCardsApiRoute)

}

trait NineCardsApi extends HttpService with SprayJsonSupport {

  import DependencyInjector._
  import FreeUtils._
  import JsonFormats._

  val nineCardsApiRoute =
    pathPrefix("users") {
      pathEndOrSingleSlash {
        post {
          complete("Starts a session")
        }
      } ~
        path(Segment) { userId =>
          get {
            complete(
              Map("result" -> s"Gets user info: $userId")
            )
          } ~
            put {
              complete(
                Map("result" -> s"Updates user info: $userId")
              )
            }
        } ~
        path("link") {
          put {
            complete(
              Map("result" -> s"Links new account with specific user")
            )
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
            val result: Id.Id[Seq[GooglePlayApp]] = appProcesses.categorizeApps(Seq("com.fortysevendeg.ninecards"))
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
}