package cards.nine.domain.application

import enumeratum.{ Enum, EnumEntry }

sealed trait Moment extends EnumEntry

object Moment extends Enum[Moment] {

  case object CAR extends Moment
  case object HOME extends Moment
  case object MUSIC extends Moment
  case object NIGHT extends Moment
  case object OUT_AND_ABOUT extends Moment
  case object SPORT extends Moment
  case object STUDY extends Moment
  case object WORK extends Moment

  val values = super.findValues

  val valuesName = values.toList map (_.entryName)

  val widgetMomentPrefix = "WIDGET_"

  val widgetValuesName = values.toList map (v ⇒ s"$widgetMomentPrefix${v.entryName}")

  def isMoment(categoryName: String): Boolean =
    Moment.withNameOption(categoryName.replace(widgetMomentPrefix, "")).fold(false)(values.contains)
}