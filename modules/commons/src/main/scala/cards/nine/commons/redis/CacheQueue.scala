package cards.nine.commons.redis

import com.redis.RedisClient
import com.redis.serialization.{ Format, Parse }

/**
  * A CacheQueue implements a queue backed in a Redis Cache. The operations assume that some keys in
  * the cache may have a value that is a Redis List (http://redis.io/topics/data-types-intro#redis-lists)
  * A queue of values of some type is managed by this class, by enqueuing on the left and dequeuing from the right.
  * The CacheQueue class is generic on the type of the Key and that of the values stored in the Queue.
  */
class CacheQueue[Key, Val](client: RedisClient)(implicit f: Format, pv: Parse[Option[Val]]) {

  // We see Redis lists as queues: we enqueue on the right, and dequeue from the left (
  // Thus, retrieving goes through positive indexes.

  def enqueue(key: Key, value: Val): Unit =
    client.rpush(key, value)

  def enqueueMany(key: Key, values: List[Val]): Unit = values match {
    case Nil ⇒ Unit
    case h :: t ⇒ client.lpush(key, h, t: _*)
  }

  def enqueueAtMany(keys: List[Key], value: Val): Unit = {
    import scala.concurrent.Await // FIXME
    import scala.concurrent.duration.Duration.Inf
    def commandForKey(key: Key): (() ⇒ Any) = {
      () ⇒ client.rpush(key, value)
    }
    client
      .pipelineNoMulti(keys map commandForKey)
      .map(a ⇒ Await.result(a.future, Inf))
  }

  def dequeue(key: Key): Option[Val] = client.lpop(key).flatten

  /* A few notes on how Redis handles indexes and ranges for lists
   *  in a non-empty list A of N elements, indexes are numbered from 0 to N-1,
   *  - so "lindex A 0" givers first element (head),
   * a negative index number indicates position counting from end,
   *  - so "lindex A -1" gives last element,
   */
  def takeMany(key: Key, num: Int): List[Val] =
    if (num <= 0)
      Nil
    else
      client.lrange[Option[Val]](key, 0, num - 1)
        .getOrElse(Nil)
        .flatMap(_.flatten.toList)

  def dequeueMany(key: Key, num: Int): Unit =
    if (num > 0)
      client.ltrim(key, num, -1) // end Index is -1
    else {}

  def length(key: Key): Long = client.llen(key).getOrElse(0)

  def delete(key: Key): Unit = client.del(key)

  def delete(keys: List[Key]): Unit = keys match {
    case head :: tail ⇒ client.del(head, tail: _*)
    case _ ⇒ Nil
  }
}

object CacheQueue {

  def apply[Key, Val](client: RedisClient)(
    implicit
    format: Format,
    keyParse: Parse[Option[Key]],
    valParse: Parse[Option[Val]]
  ): CacheQueue[Key, Val] =
    new CacheQueue[Key, Val](client)

}