package cards.nine.commons.redis

import cats.instances.future._
import cats.syntax.functor._
import scala.concurrent.{ ExecutionContext, Future }
import scredis.serialization.{ Reader, Writer }

class CacheWrapper[Key, Val](
  implicit
  format: Format[Key],
  writer: Writer[Val],
  reader: Reader[Option[Val]],
  ec: ExecutionContext
) {

  def get(key: Key): RedisOps[Option[Val]] =
    RedisOps.withRedisClient { client ⇒
      client.get[Option[Val]](format(key)).map(_.flatten)
    }

  def mget(keys: List[Key]): RedisOps[List[Val]] =
    RedisOps.withRedisClient { client ⇒
      if (keys.isEmpty)
        Future.successful(Nil)
      else
        client.mGet[Option[Val]](keys.map(format): _*).map(_.flatten.flatten)
    }

  def put(entry: (Key, Val)): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      val (key, value) = entry
      client.set[Val](format(key), value).void
    }

  def mput(entries: List[(Key, Val)]): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      if (entries.isEmpty)
        Future.successful(())
      else
        client.mSet[Val](entries.map({ case (k, v) ⇒ format(k) → v }).toMap)
    }

  def delete(key: Key): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      client.del(key).void
    }

  def delete(keys: List[Key]): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      if (keys.isEmpty) Future.successful(())
      else client.del(keys map format: _*).void
    }

}
