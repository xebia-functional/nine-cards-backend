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

case class CacheKey( `name`: Package, `type`: KeyType, date: Option[DateTime] )

case class CacheVal(card: Option[FullCard])
