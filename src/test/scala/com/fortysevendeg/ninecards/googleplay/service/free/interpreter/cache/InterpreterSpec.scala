package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.cache

import com.fortysevendeg.ninecards.googleplay.domain.{Package, FullCard}
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.TestData.fortyseven
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.cache._
import com.redis.RedisClient
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.specification.{AfterAll, BeforeAll, BeforeEach}
import org.specs2.ScalaCheck
import redis.embedded.RedisServer

class InterpreterSpec extends Specification with ScalaCheck with BeforeAll with BeforeEach with AfterAll {

  import com.fortysevendeg.ninecards.googleplay.util.ScalaCheck._
  import io.circe.syntax._
  import org.scalacheck.Shapeless._
  import org.scalacheck.Prop._
  import CirceCoders._

  private[this] val redisServer: RedisServer =
    new RedisServer()
  redisServer.start()
  private[this] val redisClient: RedisClient =
    new RedisClient( host = "localhost", port = redisServer.getPort() )

  private[this] def eval[A]( op: WithClient[A]) = op(redisClient)

  private[this] val interpreter = CacheInterpreter

  def beforeAll = {}

  def afterAll = redisServer.stop()

  def before = redisClient.flushall

  private def putEntry( e: CacheEntry) = redisClient.set( e._1.asJson.noSpaces, e._2.asJson.noSpaces)

  sequential

  "getValidCard" should {

    "return None if the Cache is empty" >>
      prop { pack: Package =>
        eval( interpreter( GetValid( pack) )) must beNone
      }

    "return None if the cache only contains Error or Pending entries for the package" >>
      prop { pack: Package =>
        putEntry( CacheEntry.error( pack, DateTime.now ) )
        putEntry( CacheEntry.error( pack, DateTime.now ) )
        eval (interpreter( GetValid( fortyseven.packageObj ) )) must beNone
      }

    "return Some(e) if the cache contains a Resolved entry" >>
      prop { card: FullCard => 
        putEntry( CacheEntry.resolved( Package(card.packageName), card ) ) 
        eval( interpreter(GetValid(Package(card.packageName))) ) must beSome(card)
      }

    "return Some(e) if the cache contains a Permanent entry" >>
      prop { card: FullCard => 
        putEntry( CacheEntry.permanent(Package(card.packageName), card) )
        eval( interpreter(GetValid( Package(card.packageName))) ) must beSome(card)
      }

  }

}
