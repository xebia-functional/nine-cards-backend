package cards.nine.commons.redis

import cards.nine.commons.catscalaz.ScalaFuture2Task
import scalaz.concurrent.Task
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
  valReader: Reader[Option[Val]]) {

  import scala.concurrent.ExecutionContext.Implicits.global

  // We see Redis lists as queues: we enqueue on the right, and dequeue from the left (
  // Thus, retrieving goes through positive indexes.

  def enqueue(key: Key, value: Val): RedisOps[Unit] =
    client ⇒ ScalaFuture2Task {
      client.rPush(keyFormat(key), value).map(_x ⇒ Unit)
    }

  def enqueueMany(key: Key, values: List[Val]): RedisOps[Unit] =
    client ⇒ {
      if (values.isEmpty)
        Task(Unit)
      else ScalaFuture2Task {
        client.rPush[Val](key, values: _*).map(x ⇒ Unit)
      }
    }

  def dequeue(key: Key): RedisOps[Option[Val]] =
    client ⇒ ScalaFuture2Task {
      client.lPop(key).map(_.flatten)
    }

  /* A few notes on how Redis handles indexes and ranges for lists
   *  in a non-empty list A of N elements, indexes are numbered from 0 to N-1,
   *  - so "lindex A 0" givers first element (head),
   * a negative index number indicates position counting from end,
   *  - so "lindex A -1" gives last element,
   */
  def takeMany(key: Key, num: Int): RedisOps[List[Val]] =
    client ⇒ {
      if (num <= 0)
        Task(Nil)
      else ScalaFuture2Task {
        client.lRange[Option[Val]](key, 0, num - 1).map(_.flatten)
      }
    }

  def dequeueMany(key: Key, num: Int): RedisOps[Unit] =
    client ⇒ {
      if (num > 0)
        ScalaFuture2Task {
          client.lTrim(key, num, -1) // end Index is -1
        }
      else Task(Unit)
    }

  def length(key: Key): RedisOps[Long] =
    client ⇒ ScalaFuture2Task {
      client.lLen(key)
    }

  def delete(key: Key): RedisOps[Unit] =
    client ⇒ ScalaFuture2Task {
      client.del(key).map(x ⇒ Unit)
    }

  def delete(keys: List[Key]): RedisOps[Unit] =
    client ⇒ {
      if (keys.isEmpty)
        Task(Unit)
      else ScalaFuture2Task {
        client.del(keys map keyFormat: _*).map(x ⇒ Unit)
      }
    }

  def purge(key: Key, value: Val): RedisOps[Unit] =
    client ⇒ ScalaFuture2Task {
      client.lRem[Val](key, value, 0).map(x ⇒ Unit)
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

    client ⇒ ScalaFuture2Task {
      client.eval[Unit, String, Val](ifExistsScript, keys, Seq(value))
    }
  }

}

object CacheQueue {

  def apply[Key, Val](
    implicit
    formK: Format[Key],
    wv: Writer[Val],
    rv: Reader[Option[Val]]
  ): CacheQueue[Key, Val] =
    new CacheQueue[Key, Val]()

}