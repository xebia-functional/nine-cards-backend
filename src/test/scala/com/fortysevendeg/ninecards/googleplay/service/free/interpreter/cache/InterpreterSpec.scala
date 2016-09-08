package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.cache

import com.fortysevendeg.ninecards.googleplay.domain.{Package, FullCard}
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.TestData.fortyseven
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.cache._
import com.redis.RedisClient
import org.joda.time.{DateTime, DateTimeZone}
import org.specs2.mutable.Specification
import org.specs2.specification.{AfterAll, BeforeAll, BeforeEach}
import org.specs2.ScalaCheck
import redis.embedded.RedisServer

class InterpreterSpec extends Specification with ScalaCheck with BeforeAll with BeforeEach with AfterAll {

  import com.fortysevendeg.ninecards.googleplay.util.ScalaCheck._
  import io.circe.syntax._
  import org.scalacheck.Shapeless._
  import CirceCoders._
  import KeyType._

  private[this] object setup {
    val redisServer: RedisServer = new RedisServer()
    redisServer.start()
    val redisClient: RedisClient = new RedisClient( host = "localhost", port = redisServer.getPort() )

    def flush = redisClient.flushall

    def eval[A]( op: WithClient[A]) = op(redisClient)

    val interpreter = CacheInterpreter

    def writeKey(t: KeyType, p: String, d: String)  =
      s"""{ "package" : "$p", "keyType" : "${t.entryName}" , "date" : $d } """.filter(_ > ' ').trim

    def resolvedKey(p: String): String = writeKey( Resolved, p, "null")
    def pendingKey(p: String) : String = writeKey( Pending, p, "null")
    def errorKey(p: String, d: String) = writeKey( Error, p, d)

    def allType( keyType: KeyType) =
      s""" { "package" : *, "keyType" : "${keyType.entryName}", "date" : * } """.filter( _ > ' ').trim

    val date: DateTime = new DateTime( 2016, 7, 23, 12, 0, 14, DateTimeZone.UTC)
    val dateJsonStr = s""" "16072312001400" """.trim

  }

  import setup._

  override def beforeAll = {}

  override def afterAll = redisServer.stop()

  override def before = flush

  sequential

  private def putEntry( e: CacheEntry) = redisClient.set( e._1.asJson.noSpaces, e._2.asJson.noSpaces)

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
        putEntry( CacheEntry.resolved( card) )
        eval( interpreter(GetValid(Package(card.packageName))) ) must beSome(card)
      }

    "return Some(e) if the cache contains a Permanent entry" >>
      prop { card: FullCard => 
        putEntry( CacheEntry.permanent(Package(card.packageName), card) )
        eval( interpreter(GetValid( Package(card.packageName))) ) must beSome(card)
      }

  }

  "putResolved" should {

    "add a package as resolved" >>
      prop { card: FullCard =>
        flush
        redisClient.get( resolvedKey(card.packageName)) must beNone
        eval(interpreter( PutResolved( card) ))
        redisClient.get( resolvedKey(card.packageName)) must beSome
      }

    "add no other key as resolved" >>
      prop { (card: FullCard, pack: Package) =>
        flush
        eval(interpreter( PutResolved(card) ))
        val actual = redisClient.get( resolvedKey(pack.value))
        if (card.packageName == pack.value) actual must beSome else actual must beNone
      }

    "add no key as pending or error" >>
      prop { card: FullCard =>
        flush
        eval(interpreter( PutResolved(card) ) )
        redisClient.keys( allType(Pending) ) must_=== Some( List() )
        redisClient.keys( allType(Error) ) must_=== Some( List() )
      }

    "overwrite any previous value" >>
      prop { (card_1: FullCard, card_2x: FullCard) =>
        flush
        val card_2 = card_2x.copy( packageName = card_1.packageName)
        eval(interpreter( PutResolved(card_1) ))
        eval(interpreter( PutResolved(card_2) ))
        val actual = redisClient.get( resolvedKey(card_1.packageName))
        redisClient.keys( allType(Resolved) ) must beSome.which (_.length === 1)
        actual must_=== Some( cacheValE( CacheVal( Some( card_2) ) ).noSpaces )
      }
  }

  "markPending" should {
    "add a package as Pending" >>
      prop { pack: Package =>
        flush
        eval(interpreter( MarkPending(pack) ) )
        redisClient.get( pendingKey(pack.value) ) must beSome
      }

    "add no key as resolved or error" >>
      prop { pack: Package =>
        flush
        eval(interpreter( MarkPending(pack) ) )
        redisClient.keys( allType(Error) ) must_=== Some( List() )
        redisClient.keys( allType(Resolved) ) must_=== Some( List() )
      }
  }

  "markError" should {
    "add a package as error" >>
      prop { pack: Package =>
        flush
        eval( interpreter( MarkError( pack, date) ) )
        val keys = redisClient.keys("*")
        redisClient.get( errorKey(pack.value, dateJsonStr )) must beSome
      }

    "add no key as resolved or pending" >>
      prop { pack: Package =>
        flush
        eval( interpreter( MarkError( pack, date) ) )
        redisClient.keys( allType(Resolved) ) must_=== Some( List() )
        redisClient.keys( allType(Pending) ) must_=== Some( List() )
      }

    val date2: DateTime = new DateTime( 2005, 11, 3, 7, 5, 59, DateTimeZone.UTC)
    val date2JsonStr = s""" "05110307055900" """.trim

    "allow adding several error entries for package with different dates" >>
      prop { pack: Package =>
        flush
        eval(interpreter( MarkError(pack, date) ))
        eval(interpreter( MarkError(pack, date2) ))
        redisClient.keys( allType(Error) ) must beSome.which (_.length === 2)
      }

  }

}
