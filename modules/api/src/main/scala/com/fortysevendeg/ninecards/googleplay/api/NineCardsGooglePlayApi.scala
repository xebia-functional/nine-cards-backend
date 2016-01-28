package com.fortysevendeg.ninecards.api

import akka.actor.Actor

import spray.httpx.SprayJsonSupport

import spray.routing._

class NineCardsGooglePlayActor extends Actor with NineCardsGooglePlayApi {

  def actorRefFactory = context

  def receive = runRoute(googlePlayApiRoute)

}

trait NineCardsGooglePlayApi extends HttpService {

  def googlePlayApiRoute: Route = packageRoute

  private[this] def packageRoute =
    path("googleplay" / "package" / Segment) { packageName =>
      complete(s"Response for [$packageName]")
    }
}
