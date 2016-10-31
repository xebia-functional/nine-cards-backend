package cards.nine.googleplay.service.free.interpreter.cache

import com.redis.RedisClientPool
import io.circe.parser._
import io.circe.{ Decoder, Encoder }
import org.specs2.mutable.Specification
import org.specs2.ScalaCheck
import redis.embedded.RedisServer

class JsonRedisCacheIntegration extends Specification with ScalaCheck {

  import io.circe.generic.auto._
  import org.scalacheck.Shapeless._
  import org.scalacheck.Prop._

  class JsonRedisCache(host: String, port: Int) {

    val clientPool = new RedisClientPool(host, port)

    def cached[A, B](f: A ⇒ B)(implicit ea: Encoder[A], eb: Encoder[B], db: Decoder[B]): A ⇒ B = { a ⇒
      val encodedA = ea(a).noSpaces

      clientPool.withClient { client ⇒
        client.get(encodedA).flatMap { anyB ⇒
          decode[B](anyB).toOption
        }.getOrElse {
          val res = f(a)
          client.set(encodedA, eb(res).noSpaces)
          res
        }
      }
    }
  }

  case class Inner(i: Int, b: Boolean)
  case class Outer(s: String, ti: Inner)

  "Caching" should {
    "only forward requests to the backend once" >> prop { testOuterSet: Set[Outer] ⇒
      val testOuterList = testOuterSet.toList

      val receivedRequests = scala.collection.mutable.ListBuffer.empty[Outer]

      val testFunction: Outer ⇒ Inner = { to ⇒
        receivedRequests += to
        to.ti
      }

      val redisServer = new RedisServer()
      val redisPort = redisServer.getPort

      val cachedFunction = new JsonRedisCache("localhost", redisPort).cached(testFunction)

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
