package com.fortysevendeg.ninecards.api

import akka.actor.Actor

import spray.httpx.SprayJsonSupport

import spray.routing._

class GooglePlayActor extends Actor with GooglePlayApi {

  def actorRefFactory = context

  def receive = runRoute(googlePlayApiRoute)

}

trait GooglePlayApi extends HttpService {

  def googlePlayApiRoute: Route = packageRoute

  private[this] def packageRoute =
    path("googleplay" / "package" / Segment) { packageName =>
      complete(s"Response for [$packageName]")
    }
}
