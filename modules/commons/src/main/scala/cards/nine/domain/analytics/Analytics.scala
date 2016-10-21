package cards.nine.domain

import cards.nine.domain.application.Package
import enumeratum.{ Enum, EnumEntry }
import org.joda.time.DateTime

package analytics {

  sealed trait Country extends EnumEntry

  object Country extends Enum[Country] {

    case object Spain extends Country

    case object United_Kingdom extends Country

    case object United_States extends Country

    val values = super.findValues
  }

  sealed trait GeoScope

  case class CountryScope(country: Country) extends GeoScope

  case object WorldScope extends GeoScope

  object CountryScope {
    def lookup(name: String): Option[CountryScope] =
      Country
        .withNameOption(name.replace(" ", "_"))
        .map(CountryScope.apply)
  }

  sealed abstract class ReportType extends EnumEntry

  object ReportType extends Enum[ReportType] {

    case object AppsRankingByCategory extends ReportType

    val values = super.findValues
  }

  case class DateRange(startDate: DateTime, endDate: DateTime)

  case class AnalyticsToken(value: String) extends AnyVal

  case class RankingParams(dateRange: DateRange, rankingLength: Int, auth: AnalyticsToken)

  case class UnrankedApp(packageName: Package, category: String)

  case class RankedApp(packageName: Package, category: String, position: Option[Int])

}