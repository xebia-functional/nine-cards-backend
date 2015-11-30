package com.fortysevendeg.ninecards.api

import akka.actor.ActorRefFactory
import org.specs2.mutable.Specification
import org.specs2.matcher.Matchers
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes
import spray.routing.{Route, HttpService}
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

  val usersPath = "/users"

  val spec = this
  val nineCardsApi = new NineCardsApi {
    override implicit def actorRefFactory: ActorRefFactory = spec.actorRefFactory
  }.nineCardsApiRoute
}

class NineCardsApiSpec
  extends NineCardsApiSpecification {
  import NineCardsApiHeaderCommons._

  "nineCardsApi" should {
    "require basic login headers for GET users" in new NineCardsScope {
      Get(usersPath + "/1111")  ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual 401
      }
      Get(usersPath + "/1111") ~> RawHeader(headerAppslyAppId, "testNineCards") ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldEqual 401
      }
      Get(usersPath + "/1111") ~> RawHeader(headerAppslyAppId, "testNineCards") ~> RawHeader(headerAppslyAPIKey, "testAPIKey") ~> sealRoute(nineCardsApi) ~> check {
        status.intValue shouldNotEqual 401
      }
    }
  }
}