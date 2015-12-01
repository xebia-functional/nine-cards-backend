package com.fortysevendeg.ninecards.api

import akka.actor.ActorRefFactory
import org.specs2.mutable.Specification
import org.specs2.matcher.Matchers
import org.specs2.specification.Scope
import spray.http.MediaTypes
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

trait NineCardsApiSpecification
  extends Specification
  with Matchers
  with Specs2RouteTest
  with HttpService {

  trait NineCardsScope
    extends Scope {

  }

  implicit def actorRefFactory = system

  val apiDocsPath = "/apiDocs/index.html"

  val spec = this
  val nineCardsApi = new NineCardsApi {
    override implicit def actorRefFactory: ActorRefFactory = spec.actorRefFactory
  }.nineCardsApiRoute
}

class NineCardsApiSpec
  extends NineCardsApiSpecification {
  "nineCardsApi" should {
    "grant access to Swagger documentation" in new NineCardsScope {
      Get(apiDocsPath) ~> sealRoute(nineCardsApi) ~> check {
        status should be equalTo 200
        mediaType === MediaTypes.`text/html`
        responseAs[String] must contain("Swagger")
      }
    }
  }
}
