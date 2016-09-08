package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.cache

import cats.Monad
import cats.syntax.all._
import com.redis._
import io.circe._
import io.circe.parser._
import scala.annotation.tailrec
import com.redis.serialization.Parse

class JsonRedisCache(host: String, port: Int) {

  val clientPool = new RedisClientPool(host, port)

  def cached[A, B](f: A => B)(implicit ea: Encoder[A], eb: Encoder[B], db: Decoder[B]): A => B = { a =>
    val encodedA = ea(a).noSpaces

    clientPool.withClient{ client =>
      client.get(encodedA).flatMap { anyB =>
        decode[B](anyB).toOption
      }.getOrElse {
        val res = f(a)
        client.set(encodedA, eb(res).noSpaces)
        res
      }
    }
  }
}

abstract class RedisCachedMonadicFunction[A,B, M[_]](
  process: A => M[B],
  clientPool: RedisClientPool)(
  implicit monad: Monad[M]
) extends (A => M[B]) {

  protected[this] type Key
  protected[this] type Val

  protected[this] implicit val encodeKey: Encoder[Key]
  protected[this] implicit val encodeVal: Encoder[Val]
  protected[this] implicit val decodeVal: Decoder[Val]
  protected[this] def extractKeys(input: A) : List[Key]
  protected[this] def extractEntry(input: A, result: B): (Key, Val)
  protected[this] def rebuildValue(input: A, value: Val): B

  def apply(input: A) : M[B] = clientPool.withClient(applyClient(input, _))

  private[this] def applyClient(input: A, client: RedisClient) : M[B] = {

    val wrap = new CacheWrapper[Key, Val](client)
    val tryLoad: Option[Val] = wrap.findFirst( extractKeys(input) )
    tryLoad match {
      case Some(cached) =>
        monad.pure( rebuildValue(input, cached) )
      case None =>
        for { // The M Monad M
          result <- process(input)
          _ = wrap.put( extractEntry(input, result) )
        } yield result
    }
  }

}

class CacheWrapper[Key, Val](client: RedisClient)
  (implicit ek: Encoder[Key], ev: Encoder[Val], dv: Decoder[Val]) {

  implicit private[this] val parseValue: Parse[Option[Val]] =
    Parse( bv => decode[Val]( Parse.Implicits.parseString( bv) ).toOption )

  def get(key: Key): Option[Val] =
    client.get[Option[Val]]( ek(key).noSpaces ).flatten

  def put( entry: (Key, Val) ): Unit =
    client.set( ek(entry._1).noSpaces, ev(entry._2).noSpaces )

  def findFirst(keys: List[Key]) : Option[Val] = {
    loopTryKeys(keys)

    @tailrec
    def loopTryKeys( keys: List[Key]): Option[Val] = keys match {
      case Nil => None
      case key :: rkeys =>
        get(key) match {
          case oval@ Some(_) => oval
          case None => loopTryKeys(rkeys)
        }
    }
    loopTryKeys(keys)
  }

}
