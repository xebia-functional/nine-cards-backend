package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.domain.application.{ FullCard, Package }
import enumeratum.{ Enum, EnumEntry }
import org.joda.time.DateTime

sealed trait KeyType extends EnumEntry
object KeyType extends Enum[KeyType] {
  case object Resolved extends KeyType
  case object Permanent extends KeyType
  case object Error extends KeyType
  case object Pending extends KeyType

  val values = super.findValues
}

case class CacheKey(`package`: Package, keyType: KeyType, date: Option[DateTime])

object CacheKey {
  import KeyType._

  def resolved(name: Package): CacheKey = CacheKey(name, Resolved, None)

  def permanent(name: Package): CacheKey = CacheKey(name, Permanent, None)

  def error(name: Package, date: DateTime): CacheKey = CacheKey(name, Error, Some(date))

  def pending(name: Package): CacheKey = CacheKey(name, Pending, None)

}

case class CacheVal(card: Option[FullCard])

object CacheEntry {

  def resolved(card: FullCard): CacheEntry =
    (CacheKey.resolved(card.packageName), CacheVal(Some(card)))

  def pending(name: Package): CacheEntry =
    (CacheKey.pending(name), CacheVal(None))

  def error(name: Package, date: DateTime): CacheEntry =
    (CacheKey.error(name, date), CacheVal(None))

  def permanent(name: Package, card: FullCard): CacheEntry =
    (CacheKey.permanent(name), CacheVal(Some(card)))
}

sealed trait JsonPattern

case object PStar extends JsonPattern

case object PNull extends JsonPattern

case class PString(value: String) extends JsonPattern

case class PObject(fields: List[(PString, JsonPattern)]) extends JsonPattern

object JsonPattern {

  def print(pattern: JsonPattern): String = pattern match {
    case PStar ⇒ """ * """.trim
    case PNull ⇒ """ null """.trim
    case PString(str) ⇒ s""" "$str" """.trim
    case PObject(fields) ⇒
      def printField(field: (PString, JsonPattern)): String = {
        val key = print(field._1)
        val value = print(field._2)
        s""" $key:$value """.trim
      }

      val fs = fields.map(printField).mkString(",")
      s""" {$fs} """.trim
  }

}
