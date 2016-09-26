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

    case UnmarkPending(pack) => {client: RedisClient =>
      val wrap = new CacheWrapper[CacheKey, CacheVal](client)
      wrap.delete( CacheKey.pending(pack) )
    }

    case MarkError(pack, date) => { client: RedisClient =>
      val wrap = new CacheWrapper[CacheKey, CacheVal](client)
      wrap.put( CacheEntry.error(pack, date) )
    }

    case ClearInvalid(pack) => { client: RedisClient =>
      val errorsPattern : JsonPattern = PObject( List(
        PString( "package") -> PString(pack.value),
        PString( "keyType") -> PString(KeyType.Error.entryName),
        PString( "date") -> PStar
      ))
      val wrap = new CacheWrapper[CacheKey, CacheVal](client)
      wrap.matchKeys(errorsPattern).foreach(wrap.delete)
      wrap.delete(CacheKey.pending(pack))
    }

    case IsPending(pack) => { client: RedisClient =>
      val wrap = new CacheWrapper[CacheKey, CacheVal](client)
      wrap.get(CacheKey.pending(pack)).isDefined
    }

    case ListPending(num) => { client: RedisClient =>
      val wrap = new CacheWrapper[CacheKey, CacheVal](client)
      val pendingPattern : JsonPattern = PObject( List(
        PString( "package") -> PStar,
        PString( "keyType") -> PString(KeyType.Pending.entryName),
        PString( "date") -> PNull
      ))
      wrap.matchKeys(pendingPattern)
        .take(num).map( _.`package`)
    }

  }

}