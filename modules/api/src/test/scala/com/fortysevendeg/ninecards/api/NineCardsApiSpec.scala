package com.fortysevendeg.ninecards.api

import akka.actor.ActorRefFactory
import org.specs2.mutable.Specification
import org.specs2.matcher.Matchers
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

trait NineCardsApiSpecification
  extends Specification
  with Matchers
  with Specs2RouteTest
  with HttpService
  with AuthHeadersRejectionHandler {

  trait NineCardsScope extends Scope

  implicit def actorRefFactory = system

  val usersPath = "/login"
  val apiDocsPath = "/apiDocs/index.html"

  val spec = this
  val nineCardsApi = new NineCardsApi {
    override implicit def actorRefFactory: ActorRefFactory = spec.actorRefFactory
  }.nineCardsApiRoute
}

class NineCardsApiSpec
  extends NineCardsApiSpecification {
  import NineCardsApiHeaderCommons._

  "nineCardsApi" should {
    "grant access to Swagger documentation" in new NineCardsScope {
      Get(apiDocsPath) ~> sealRoute(nineCardsApi) ~> check {
        status should be equalTo 200
        mediaType === MediaTypes.`text/html`
        responseAs[String] must contain("Swagger")
      }
    }

    "require basic login headers for POST users" in new NineCardsScope {
      Post(usersPath)  ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual 401
      }
      Post(usersPath) ~> RawHeader(headerAppslyAppId, "testNineCards") ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual 401
      }
      Post(usersPath) ~> RawHeader(headerAppslyAppId, "testNineCards") ~> RawHeader(headerAppslyAPIKey, "testAPIKey") ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldNotEqual 401
      }
    }
  }
}