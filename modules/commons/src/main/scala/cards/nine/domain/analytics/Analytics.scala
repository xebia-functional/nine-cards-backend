/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.domain

import cards.nine.domain.application.{ Package, Widget }
import enumeratum.{ Enum, EnumEntry }
import org.joda.time.DateTime

package analytics {

  object Values {
    val maxReportsPerRequest = 5
  }

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