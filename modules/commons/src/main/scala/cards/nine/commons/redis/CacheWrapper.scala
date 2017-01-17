package cards.nine.commons.redis

import cards.nine.commons.catscalaz.ScalaFuture2Task
import scala.concurrent.ExecutionContext
import scredis.serialization.{ Reader, Writer }
import scalaz.concurrent.Task

class CacheWrapper[Key, Val](
  implicit
  format: Format[Key],
  writer: Writer[Val],
  reader: Reader[Option[Val]],
  ec: ExecutionContext
) {

  def get(key: Key): RedisOps[Option[Val]] =
    client ⇒ ScalaFuture2Task {
      client.get[Option[Val]](format(key)).map(_.flatten)
    }

  def mget(keys: List[Key]): RedisOps[List[Val]] =
    client ⇒ {
      if (keys.isEmpty)
        Task(Nil)
      else
        ScalaFuture2Task {
          client.mGet[Option[Val]](keys.map(format): _*).map(_.flatten.flatten)
        }
    }

  def put(entry: (Key, Val)): RedisOps[Unit] =
    client ⇒ ScalaFuture2Task {
      val (key, value) = entry
      client.set[Val](format(key), value).map(x ⇒ Unit)
    }

  def mput(entries: List[(Key, Val)]): RedisOps[Unit] =
    client ⇒ {
      if (entries.isEmpty)
        Task(Unit)
      else ScalaFuture2Task {
        client.mSet[Val](entries.map({ case (k, v) ⇒ format(k) → v }).toMap)
      }
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
        client.del(keys map format: _*).map(x ⇒ Unit)
      }
    }

}
