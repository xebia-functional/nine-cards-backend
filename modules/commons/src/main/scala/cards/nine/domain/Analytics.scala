package cards.nine.domain.analytics

import enumeratum.{ Enum, EnumEntry }
import org.joda.time.DateTime

sealed trait Continent extends EnumEntry
object Continent extends Enum[Continent] {
  case object Americas extends Continent
  case object Africa extends Continent
  case object Asia extends Continent
  case object Europe extends Continent
  case object Oceania extends Continent

  val values = super.findValues
}

sealed trait Country extends EnumEntry
object Country extends Enum[Country] {
  case object Spain extends Country
  case object United_Kingdom extends Country
  case object United_States extends Country

  val values = super.findValues
}

sealed trait GeoScope
case class CountryScope(country: Country) extends GeoScope
case class ContinentScope(continent: Continent) extends GeoScope
case object WorldScope extends GeoScope

object CountryScope {
  def lookup(name: String): Option[CountryScope] =
    Country
      .withNameOption(name.replace(" ", "_"))
      .map(CountryScope.apply)
}

object ContinentScope {
  def lookup(name: String): Option[ContinentScope] =
    Continent
      .withNameOption(name.replace(" ", "_"))
      .map(ContinentScope.apply)
}

/* A date range specifies a contiguous set of days: startDate, startDate + 1 day, ..., endDate.
 https://developers.google.com/analytics/devguides/reporting/core/v4/rest/v4/reports/batchGet#DateRange */
case class DateRange(startDate: DateTime, endDate: DateTime)

case class AnalyticsToken(value: String) extends AnyVal

