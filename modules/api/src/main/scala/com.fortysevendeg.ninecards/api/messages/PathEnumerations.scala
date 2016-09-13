package com.fortysevendeg.ninecards.api.messages

import enumeratum.{ Enum, EnumEntry }

object PathEnumerations {

  sealed trait PriceFilter extends EnumEntry

  object PriceFilter extends Enum[PriceFilter] {
    case object ALL extends PriceFilter
    case object FREE extends PriceFilter
    case object PAID extends PriceFilter

    val values = super.findValues
  }

}
