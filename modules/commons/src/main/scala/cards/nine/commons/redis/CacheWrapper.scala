package cards.nine.commons.redis

import com.redis.RedisClient
import com.redis.serialization.{ Format, Parse }

class CacheWrapper[Key, Val](client: RedisClient)(implicit f: Format, pk: Parse[Option[Key]], pv: Parse[Option[Val]]) {

  def get(key: Key): Option[Val] = client.get[Option[Val]](key).flatten

  def mget(keys: List[Key]): List[Val] = keys match {
    case head :: tail ⇒ client.mget[Option[Val]](head, tail: _*).getOrElse(Nil).flatMap(_.flatten.toList)
    case _ ⇒ Nil
  }

  def put(entry: (Key, Val)): Unit = client.set(entry._1, entry._2)

  def mput(entries: List[(Key, Val)]): Unit = entries match {
    case Nil ⇒ Unit
    case _ ⇒ client.mset(entries: _*)
  }

  def findFirst(keys: List[Key]): Option[Val] = keys.toStream.map(get).find(_.isDefined).flatten

  def matchKeys(pattern: JsonPattern): List[Key] =
    matchKeys(JsonPattern.print(pattern))

  def matchKeys(pattern: String): List[Key] =
    client
      .keys[Option[Key]](pattern)
      .getOrElse(Nil)
      .flatMap(_.flatten.toList)

  def delete(key: Key): Unit = client.del(key)

  def delete(keys: List[Key]): Unit = keys match {
    case head :: tail ⇒ client.del(head, tail: _*)
    case _ ⇒ Nil
  }
}

object CacheWrapper {

  def apply[Key, Val](client: RedisClient)(
    implicit
    format: Format,
    keyParse: Parse[Option[Key]],
    valParse: Parse[Option[Val]]
  ): CacheWrapper[Key, Val] =
    new CacheWrapper[Key, Val](client)
}
