package com.fortysevendeg.cache.redis

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
