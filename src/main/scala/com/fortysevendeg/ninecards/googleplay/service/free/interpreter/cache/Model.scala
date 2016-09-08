package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.cache

import com.fortysevendeg.ninecards.googleplay.domain.{FullCard, Package}
import enumeratum.{Enum, EnumEntry}
import org.joda.time.DateTime

sealed trait KeyType extends EnumEntry
object KeyType extends Enum[KeyType] {
  case object Resolved extends KeyType
  case object Permanent extends KeyType
  case object Error extends KeyType
  case object Pending extends KeyType

  val values = super.findValues
}

case class CacheKey( `package`: Package, keyType: KeyType, date: Option[DateTime] )

object CacheKey {
  import KeyType._

  def resolved( name: Package): CacheKey = CacheKey( name, Resolved, None)

  def permanent(name: Package): CacheKey = CacheKey( name, Permanent, None)

  def error( name: Package, date: DateTime): CacheKey = CacheKey(name, Error, Some(date))

  def pending(name: Package) : CacheKey = CacheKey(name, Pending, None)

}

case class CacheVal(card: Option[FullCard])

object CacheEntry {
  type CacheEntry = (CacheKey, CacheVal)

  def resolved( name: Package, card: FullCard): CacheEntry =
    ( CacheKey.resolved(name), CacheVal( Some(card) ) )

  def pending( name: Package) : CacheEntry =
    ( CacheKey.pending(name), CacheVal(None) )

  def error( name: Package, date: DateTime): CacheEntry =
    ( CacheKey.error(name, date) , CacheVal(None) )

  def permanent(name: Package, card: FullCard) : CacheEntry =
    ( CacheKey.permanent(name), CacheVal(Some( card) ) )
}
