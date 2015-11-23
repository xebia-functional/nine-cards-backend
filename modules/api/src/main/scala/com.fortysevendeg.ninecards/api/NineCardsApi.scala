package com.fortysevendeg.ninecards.api

import akka.actor.Actor
import com.fortysevendeg.ninecards.processes.AppProcesses._
import com.fortysevendeg.ninecards.processes.domain.GooglePlayApp
import spray.httpx.SprayJsonSupport
import spray.routing._

import scala.language.{higherKinds, implicitConversions}
import scalaz.concurrent.Task

class NineCardsApiActor extends Actor with NineCardsApi {

  def actorRefFactory = context

  def receive = runRoute(nineCardsApiRoute)

}

trait NineCardsApi extends HttpService with SprayJsonSupport {

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
            complete(s"Gets user info: $userId")
          } ~
            put {
              complete(s"Updates user info: $userId")
            }
        } ~
        path("link") {
          put {
            complete(s"Links new account with specific user")
          }
        } ~
        path("installations") {
          post {
            complete(s"Creates new installation")
          }
        }
    } ~
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
}