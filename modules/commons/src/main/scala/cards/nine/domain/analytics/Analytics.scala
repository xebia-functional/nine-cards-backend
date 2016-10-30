package cards.nine.domain

import cards.nine.domain.application.Package
import enumeratum.{ Enum, EnumEntry }
import org.joda.time.DateTime

package analytics {

  import cards.nine.domain.application.Widget

  case class CountryIsoCode(value: String) extends AnyVal

  case class CountryName(value: String) extends AnyVal

  sealed abstract class GeoScope extends Product with Serializable

  case class CountryScope(code: CountryIsoCode) extends GeoScope

  case object WorldScope extends GeoScope

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

  case class RankedAppsByCategory(category: String, packages: List[RankedApp])

  case class RankedWidget(widget: Widget, moment: String, position: Option[Int])

  case class RankedWidgetsByMoment(moment: String, widgets: List[RankedWidget])

  case class UpdateRankingSummary(countryCode: Option[CountryIsoCode], created: Int)
}