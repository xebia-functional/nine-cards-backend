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
package cards.nine.services.free.interpreter.analytics

import cards.nine.commons.NineCardsErrors.ReportNotFound
import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.analytics.{ CountryIsoCode, DateRange }
import cards.nine.domain.analytics.Values._
import cards.nine.domain.application.Package
import cards.nine.services.free.domain.Ranking.CountriesWithRanking
import cards.nine.services.free.interpreter.analytics.model.DimensionFilter._
import cards.nine.services.free.interpreter.analytics.model._
import cards.nine.services.free.interpreter.analytics.model.order.OrderBy
import cats.syntax.either._

object HttpMessagesFactory {

  object CountriesWithRankingReport {
    def buildRequest(
      dateRange: DateRange,
      viewId: String
    ): RequestBody =
      RequestBody(
        ReportRequest(
          viewId     = viewId,
          dateRanges = List(dateRange),
          dimensions = List(Dimension.countryIsoCode),
          metrics    = List(Metric.eventValue),
          orderBys   = List(OrderBy.countryIsoCode)
        )
      )

    def parseResponse(response: ResponseBody): Result[CountriesWithRanking] =
      Either.fromOption(response.reports.headOption, ReportNotFound("Report not found")) map {
        report ⇒
          CountriesWithRanking(
            report.data.rows.getOrElse(Nil)
              .flatMap(_.dimensions.headOption)
              .map(CountryIsoCode)
          )
      }
  }

  object RankingsByCountryReport {

    def buildRequestsForCountry(
      code: Option[CountryIsoCode],
      categories: List[String],
      dateRange: DateRange,
      rankingSize: Int,
      viewId: String
    ): Iterator[RequestBody] = {
      categories
        .map(buildRequestForCountryAndCategory(code, dateRange, rankingSize, viewId))
        .grouped(maxReportsPerRequest)
        .map(list ⇒ RequestBody(list))
    }

    def buildRequestForCountryAndCategory(
      code: Option[CountryIsoCode],
      dateRange: DateRange,
      rankingSize: Int,
      viewId: String
    )(category: String): ReportRequest =
      ReportRequest(
        viewId                 = viewId,
        dateRanges             = List(dateRange),
        dimensions             = List(Dimension.category, Dimension.packageName),
        dimensionFilterClauses = dimensionFiltersFor(code, category),
        metrics                = List(Metric.eventValue),
        orderBys               = List(OrderBy.category, OrderBy.eventValue),
        pageSize               = rankingSize
      )

    def parseResponse(response: ResponseBody): List[(String, List[Package])] =
      response.reports.flatMap(parseReport)

    def parseReport(report: Report): List[(String, List[Package])] =
      report.data.rows.getOrElse(Nil)
        .map(toDimensionCell)
        .groupBy(_.category)
        .mapValues(_ map (_.packageName))
        .toList

    private def dimensionFiltersFor(code: Option[CountryIsoCode], category: String): DimensionFilter.Clauses =
      singleClause(Filter.isCategory(category)) ++
        code.fold(List.empty[Clause])(isoCode ⇒ singleClause(Filter.isCountry(isoCode)))

    case class DimensionCell(category: String, packageName: Package)

    private def toDimensionCell(row: ReportRow): DimensionCell = {
      val List(category, packageName) = row.dimensions
      DimensionCell(category, Package(packageName))
    }
  }
}
