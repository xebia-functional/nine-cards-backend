package cards.nine.googleplay.service.free.interpreter.cache

import com.redis._
import com.redis.serialization.{ Format, Parse }
import io.circe._
import io.circe.parser._

import scala.annotation.tailrec

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
    client
      .keys[Option[Key]](JsonPattern.print(pattern))
      .getOrElse(Nil)
      .flatMap(_.flatten.toList)

  def delete(key: Key): Unit = client.del(key)

  def delete(keys: List[Key]): Unit = keys match {
    case head :: tail ⇒ client.del(head, tail: _*)
    case _ ⇒ Nil
  }
}

object CacheWrapper {

  implicit def keyParse(implicit dv: Decoder[CacheKey]): Parse[Option[CacheKey]] =
    Parse(bv ⇒ decode[CacheKey](Parse.Implicits.parseString(bv)).toOption)

  implicit def valParse(implicit dv: Decoder[CacheVal]): Parse[Option[CacheVal]] =
    Parse(bv ⇒ decode[CacheVal](Parse.Implicits.parseString(bv)).toOption)

  implicit def keyAndValFormat(implicit ek: Encoder[CacheKey], ev: Encoder[CacheVal]): Format =
    Format {
      case key: CacheKey ⇒ ek(key).noSpaces
      case value: CacheVal ⇒ ev(value).noSpaces
    }

  def apply[Key, Val](client: RedisClient)(
    implicit
    format: Format,
    keyParse: Parse[Option[Key]],
    valParse: Parse[Option[Val]]
  ): CacheWrapper[Key, Val] =
    new CacheWrapper[Key, Val](client)
}
