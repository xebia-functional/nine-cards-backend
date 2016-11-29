package cards.nine.commons.redis

import cards.nine.commons.catscalaz.ScalaFuture2Task
import scalaz.concurrent.Task
import scredis.serialization.{ Reader, Writer }

class CacheSet[Key, Elem](key: Key)(implicit
  keyFormat: Format[Key],
  valWriter: Writer[Elem],
  valReader: Reader[Option[Elem]]) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def insert(elem: Elem): RedisOps[Unit] =
    client ⇒ ScalaFuture2Task {
      client.sAdd[Elem](keyFormat(key), elem).map(x ⇒ Unit)
    }

  def insert(elems: List[Elem]): RedisOps[Unit] =
    client ⇒ {
      if (elems.isEmpty)
        Task(Unit)
      else ScalaFuture2Task {
        client.sAdd[Elem](keyFormat(key), elems: _*)
      }.map(x ⇒ Unit)
    }

  def remove(elem: Elem): RedisOps[Unit] =
    client ⇒ ScalaFuture2Task {
      client.sRem[Elem](keyFormat(key), elem).map(x ⇒ Unit)
    }

  def remove(elems: List[Elem]): RedisOps[Unit] =
    client ⇒ {
      if (elems.isEmpty)
        Task(Unit)
      else ScalaFuture2Task {
        client.sRem[Elem](keyFormat(key), elems: _*).map(x ⇒ Unit)
      }
    }

  def contains(elem: Elem): RedisOps[Boolean] =
    client ⇒ ScalaFuture2Task {
      client.sIsMember(keyFormat(key), elem)
    }

  def extractMany(num: Int): RedisOps[List[Elem]] = {
    import cats.syntax.functor._
    import RedisOps.applicative

    RedisOps.applicative.replicateA(num, extractOne).map(_.flatten)
  }

  val extractOne: RedisOps[Option[Elem]] =
    client ⇒ ScalaFuture2Task {
      client.sPop[Option[Elem]](keyFormat(key)).map(_.flatten)
    }

}
