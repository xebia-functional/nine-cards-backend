package com.fortysevendeg.cache.redis

import cats.Monad
import cats.syntax.all._
import com.redis._
import io.circe._
import io.circe.parser._

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

  protected[this] type CacheKey
  protected[this] type CacheVal

  protected[this] def extractKey(input: A) : CacheKey
  protected[this] def extractValue(input: A, result: B): CacheVal
  protected[this] def rebuildValue(input: A, value: CacheVal): B

  protected[this] implicit val encodeKey: Encoder[CacheKey]
  protected[this] implicit val encodeVal: Encoder[CacheVal]
  protected[this] implicit val decodeVal: Decoder[CacheVal]

  private[this] def applyClient(input: A, client: RedisClient) : M[B] = {
    val encodedKey = encodeKey( extractKey(input) ).noSpaces
    val tryLoad: Option[B] =
      for { // Option Monad
        rawVal <- client.get(encodedKey)
        decodedVal <- decode[CacheVal](rawVal).toOption
      } yield rebuildValue(input, decodedVal)

    tryLoad match {
      case Some(result) =>
        monad.pure(result)
      case None =>
        for { // The M Monad M
          result <- process(input)
          cachedValue = extractValue(input, result)
          _ = client.set(encodedKey, encodeVal(cachedValue).noSpaces)
        } yield result
    }
  }

  def apply(input: A) : M[B] = clientPool.withClient(applyClient(input, _))

}