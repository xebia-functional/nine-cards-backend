package cards.nine.googleplay.service.free.interpreter.cache

import cats.Monad
import cats.syntax.all._
import com.redis._
import io.circe._
import io.circe.parser._
import scala.annotation.tailrec
import com.redis.serialization.Parse

class JsonRedisCache(host: String, port: Int) {

  val clientPool = new RedisClientPool(host, port)

  def cached[A, B](f: A => B)(implicit ea: Encoder[A], eb: Encoder[B], db: Decoder[B]): A => B = { a =>
    val encodedA = ea(a).noSpaces

    clientPool.withClient{ client =>
      client.get(encodedA).flatMap { anyB =>
        decode[B](anyB).toOption
      }.getOrElse {
        val res = f(a)
        client.set(encodedA, eb(res).noSpaces)
        res
      }
    }
  }
}

class CacheWrapper[Key, Val](client: RedisClient)
  (implicit ek: Encoder[Key], dk: Decoder[Key], ev: Encoder[Val], dv: Decoder[Val]) {

  implicit private[this] val parseValue: Parse[Option[Val]] =
    Parse( bv => decode[Val]( Parse.Implicits.parseString( bv) ).toOption )

  def get(key: Key): Option[Val] =
    client.get[Option[Val]]( ek(key).noSpaces ).flatten

  def put( entry: (Key, Val) ): Unit =
    client.set( ek(entry._1).noSpaces, ev(entry._2).noSpaces )

  def findFirst(keys: List[Key]) : Option[Val] = {
    loopTryKeys(keys)

    @tailrec
    def loopTryKeys( keys: List[Key]): Option[Val] = keys match {
      case Nil => None
      case key :: rkeys =>
        get(key) match {
          case oval@ Some(_) => oval
          case None => loopTryKeys(rkeys)
        }
    }
    loopTryKeys(keys)
  }

  def matchKeys( pattern: JsonPattern): List[Key] =
    client
      .keys[String]( JsonPattern.print(pattern) )
      .getOrElse( List() )
      .flatten
      .flatMap( s => decode[Key](s).toOption )

  def delete(key: Key) : Unit =
    client.del( ek(key).noSpaces)
}

sealed trait JsonPattern
case object PStar extends JsonPattern
case object PNull extends JsonPattern
case class PString(value: String) extends JsonPattern
case class PObject(fields: List[(PString, JsonPattern)]) extends JsonPattern

object JsonPattern {

  def print(pattern: JsonPattern): String = pattern match {
    case PStar => """ * """.trim
    case PNull => """ null """.trim
    case PString(str) => s""" "$str" """.trim
    case PObject(fields) =>
      def printField( field: (PString, JsonPattern)) : String = {
        val key = print(field._1)
        val value = print(field._2)
        s""" ${key}:${value} """.trim
      }
      val fs = fields.map(printField).mkString(",")
      s""" {${fs}} """.trim
  }

}
