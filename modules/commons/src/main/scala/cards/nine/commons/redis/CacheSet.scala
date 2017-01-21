package cards.nine.commons.redis

import cats.instances.future._
import cats.syntax.functor._
import scala.concurrent.{ ExecutionContext, Future }
import scredis.serialization.{ Reader, Writer }

class CacheSet[Key, Elem](key: Key)(implicit
  keyFormat: Format[Key],
  valWriter: Writer[Elem],
  valReader: Reader[Option[Elem]],
  ec: ExecutionContext) {

  def insert(elem: Elem): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      client.sAdd[Elem](keyFormat(key), elem).void
    }

  def insert(elems: List[Elem]): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      if (elems.isEmpty) Future.successful(())
      else client.sAdd[Elem](keyFormat(key), elems: _*).void
    }

  def remove(elem: Elem): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      client.sRem[Elem](keyFormat(key), elem).void
    }

  def remove(elems: List[Elem]): RedisOps[Unit] =
    RedisOps.withRedisClient { client ⇒
      if (elems.isEmpty) Future.successful(())
      else client.sRem[Elem](keyFormat(key), elems: _*).void
    }

  def contains(elem: Elem): RedisOps[Boolean] =
    RedisOps.withRedisClient { client ⇒
      client.sIsMember(keyFormat(key), elem)
    }

  def extractMany(num: Int): RedisOps[List[Elem]] = {
    import cards.nine.commons.catscalaz.TaskInstances._

    RedisOps.applicative.replicateA(num, extractOne).map(_.flatten)
  }

  val extractOne: RedisOps[Option[Elem]] =
    RedisOps.withRedisClient { client ⇒
      client.sPop[Option[Elem]](keyFormat(key)).map(_.flatten)
    }

}
