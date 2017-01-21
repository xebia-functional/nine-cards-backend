package cards.nine.commons.redis

import cats.instances.future._
import cats.syntax.functor._
import scala.concurrent.{ ExecutionContext, Future }
import scredis.protocol.Decoder
import scredis.serialization.{ Reader, Writer }

/**
  * A CacheQueue implements a queue backed in a Redis Cache. The operations assume that some keys in
  * the cache may have a value that is a Redis List (http://redis.io/topics/data-types-intro#redis-lists)
  * A queue of values of some type is managed by this class, by enqueuing on the left and dequeuing from the right.
  * The CacheQueue class is generic on the type of the Key and that of the values stored in the Queue.
  */
class CacheQueue[Key, Val](implicit
  keyFormat: Format[Key],
  valWriter: Writer[Val],
  valReader: Reader[Option[Val]],
  ec: ExecutionContext) {

  // We see Redis lists as queues: we enqueue on the right, and dequeue from the left (
  // Thus, retrieving goes through positive indexes.

  def enqueue(key: Key, value: Val): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      client.rPush(keyFormat(key), value).void
    }

  def enqueueMany(key: Key, values: List[Val]): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      if (values.isEmpty)
        Future.successful(())
      else
        client.rPush[Val](key, values: _*).void
    }

  def dequeue(key: Key): RedisOps[Option[Val]] =
    RedisOps.withRedisClient { client ⇒
      client.lPop(key).map(_.flatten)
    }

  /* A few notes on how Redis handles indexes and ranges for lists
   *  in a non-empty list A of N elements, indexes are numbered from 0 to N-1,
   *  - so "lindex A 0" givers first element (head),
   * a negative index number indicates position counting from end,
   *  - so "lindex A -1" gives last element,
   */
  def takeMany(key: Key, num: Int): RedisOps[List[Val]] =
    RedisOps.withRedisClient { client ⇒
      if (num <= 0)
        Future.successful(Nil)
      else
        client.lRange[Option[Val]](key, 0, num - 1).map(_.flatten)
    }

  def dequeueMany(key: Key, num: Int): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      if (num > 0)
        client.lTrim(key, num, -1) // end Index is -1
      else Future.successful(())
    }

  def length(key: Key): RedisOps[Long] =
    RedisOps.withRedisClient { client ⇒
      client.lLen(key)
    }

  def delete(key: Key): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      client.del(key).void
    }

  def delete(keys: List[Key]): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      if (keys.isEmpty)
        Future.successful(())
      else
        client.del(keys map keyFormat: _*).void
    }

  def purge(key: Key, value: Val): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      client.lRem[Val](key, value, 0).void
    }

  def enqueueIfNotExists[Guard](queueKey: Key, guardKey: Guard, value: Val)(implicit guardFormat: Format[Guard]): RedisOps[Unit] = {
    // For Boolean conditions within system, can use Lua Scripts.
    val ifExistsScript = s"""
      | if redis.call("exists", KEYS[2]) == 0
      | then redis.call("rpush", KEYS[1], ARGV[1])
      | end
    """.stripMargin

    implicit val unitDecoder: Decoder[Unit] = { case x ⇒ Unit }
    val keys = Seq(keyFormat(queueKey), guardFormat(guardKey))

    RedisOps.withRedisClient { client ⇒
      client.eval[Unit, String, Val](ifExistsScript, keys, Seq(value))
    }
  }

}
