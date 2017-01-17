package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.commons.redis.TestUtils
import cats.syntax.either._
import io.circe.parser._
import io.circe.{ Decoder, Encoder }
import org.specs2.mutable.Specification
import org.specs2.ScalaCheck
import org.specs2.specification.{ AfterAll, BeforeAll }
import redis.embedded.RedisServer
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scredis.{ Client ⇒ ScredisClient }

class JsonRedisCacheIntegration extends Specification with ScalaCheck with AfterAll with BeforeAll {

  import io.circe.generic.auto._
  import org.scalacheck.Shapeless._
  import org.scalacheck.Prop._
  import TestUtils.redisTestActorSystem

  private[this] val redisServer = new RedisServer()
  private[this] val redisClient: ScredisClient = ScredisClient(host = "localhost", port = redisServer.getPort)

  override def beforeAll = redisServer.start()
  override def afterAll = redisServer.stop()

  def await[A](fut: Future[A]): A = Await.result(fut, Duration.Inf)

  def cached[A, B](f: A ⇒ B)(implicit ea: Encoder[A], eb: Encoder[B], db: Decoder[B]): A ⇒ B = { a ⇒
    val encodedA = ea(a).noSpaces
    await {
      val f1: Future[Option[B]] = for /*Future*/ {
        opt ← redisClient.get(encodedA)
        dec = opt.flatMap(b ⇒ decode[B](b).toOption)
      } yield dec
      f1 flatMap {
        case Some(b) ⇒ Future(b)
        case None ⇒
          val b = f(a)
          redisClient.set(encodedA, eb(b).noSpaces).map(_ ⇒ b)
      }
    }
  }

  case class Inner(i: Int, b: Boolean)
  case class Outer(s: String, ti: Inner)

  "Caching" should {
    "only forward requests to the backend once" >> prop { testOuterSet: Set[Outer] ⇒
      await(redisClient.flushAll)

      val testOuterList = testOuterSet.toList

      val receivedRequests = scala.collection.mutable.ListBuffer.empty[Outer]

      val testFunction: Outer ⇒ Inner = { to ⇒
        receivedRequests += to
        to.ti
      }

      val cachedFunction = cached(testFunction)

      val firstPass = testOuterList.map(cachedFunction(_))
      val secondPass = testOuterList.map(cachedFunction(_))

      val expected = receivedRequests.map(_.ti).toList

      (expected ?= firstPass) &&
        (firstPass ?= secondPass) &&
        (receivedRequests.toList ?= testOuterList)
    }
  }
}
