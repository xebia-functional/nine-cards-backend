package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.domain.application.{ FullCard, Package }
import enumeratum.{ Enum, EnumEntry }

sealed trait KeyType extends EnumEntry
object KeyType extends Enum[KeyType] {
  case object Resolved extends KeyType
  case object Permanent extends KeyType
  case object Error extends KeyType
  case object Pending extends KeyType

  val values = super.findValues
}

case class CacheKey(`package`: Package, keyType: KeyType)

object CacheKey {
  import KeyType._

  def resolved(name: Package): CacheKey = CacheKey(name, Resolved)

  def permanent(name: Package): CacheKey = CacheKey(name, Permanent)

  def pending(name: Package): CacheKey = CacheKey(name, Pending)

  def error(name: Package): CacheKey = CacheKey(name, Error)
}

case class CacheVal(card: Option[FullCard])

object CacheEntry {

  def resolved(card: FullCard): (CacheKey, CacheVal) =
    CacheKey.resolved(card.packageName) → CacheVal(Some(card))

  def pending(name: Package): (CacheKey, CacheVal) =
    CacheKey.pending(name) → CacheVal(None)

  def permanent(card: FullCard): (CacheKey, CacheVal) =
    CacheKey.permanent(card.packageName) → CacheVal(Some(card))
}
