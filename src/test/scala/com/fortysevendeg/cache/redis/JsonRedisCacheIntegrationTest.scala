package com.fortysevendeg.cache.redis

import org.specs2.mutable.Specification
import org.specs2.ScalaCheck
import org.scalacheck.Shapeless._
import org.scalacheck.Prop._
import io.circe.generic.auto._

import redis.embedded.RedisServer

case class Inner(i: Int, b: Boolean)
case class Outer(s: String, ti: Inner)

class JsonRedisCacheIntegrationTest extends Specification with ScalaCheck {

  "Caching" should {
    "only forward requests to the backend once" >> prop { testOuterSet: Set[Outer] =>
      val testOuterList = testOuterSet.toList

      val receivedRequests = scala.collection.mutable.ListBuffer.empty[Outer]

      val testFunction: Outer => Inner = { to =>
        receivedRequests += to
        to.ti
      }

      val cachedFunction = (new JsonRedisCache("localhost", 6379)).cached(testFunction)

      val redisServer = new RedisServer(6379)
      redisServer.start()

      val firstPass = testOuterList.map(cachedFunction(_))
      val secondPass = testOuterList.map(cachedFunction(_))

      redisServer.stop()

      val expected = receivedRequests.map(_.ti).toList

      (expected ?= firstPass) &&
      (firstPass ?= secondPass) &&
      (receivedRequests.toList ?= testOuterList)
    }
  }
}
