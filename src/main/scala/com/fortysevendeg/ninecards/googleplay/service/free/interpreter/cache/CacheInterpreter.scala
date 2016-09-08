package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.cache

import cats.~>
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.cache._
import com.redis.RedisClient

object CacheInterpreter extends (Ops ~> WithClient) {

  import CirceCoders._

  def apply[A](ops: Ops[A]): WithClient[A] = ops match {

    case GetValid(pack) => { client: RedisClient =>
      val wrap = new CacheWrapper[CacheKey, CacheVal](client)
      val keys = List( CacheKey.resolved(pack), CacheKey.permanent(pack) )
      wrap.findFirst(keys).flatMap( _.card)
    }

    case PutResolved(card) => { client: RedisClient =>
      val wrap = new CacheWrapper[CacheKey, CacheVal](client)
      wrap.put( CacheEntry.resolved( card ) )
    }

    case MarkPending(pack) => { client: RedisClient =>
      val wrap = new CacheWrapper[CacheKey, CacheVal](client)
      wrap.put( CacheEntry.pending( pack) )
    }

    case MarkError(pack, date) => { client: RedisClient =>
      val wrap = new CacheWrapper[CacheKey, CacheVal](client)
      wrap.put( CacheEntry.error(pack, date) )
    }

  }

}