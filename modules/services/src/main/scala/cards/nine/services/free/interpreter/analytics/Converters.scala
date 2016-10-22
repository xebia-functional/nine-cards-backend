package cards.nine.services.free.interpreter.analytics

import cards.nine.commons.NineCardsErrors.ReportNotFound
import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.analytics._
import cards.nine.domain.application.Package
import cards.nine.services.free.domain.Ranking.GoogleAnalyticsRanking
import cards.nine.services.free.interpreter.analytics.model.DimensionFilter._
import cats.syntax.either._

object Converters {

  import model._

  type Cell = (String, Package)

  def parseRanking(
    response: ResponseBody,
    rankingSize: Int,
    name: Option[CountryName]
  ): Result[GoogleAnalyticsRanking] = {
    def buildScore(cells: List[Cell]): List[Package] = cells.map(_._2).take(rankingSize)

    Either.fromOption(response.reports.headOption, ReportNotFound("Report not found")) map {
      report ⇒
        GoogleAnalyticsRanking(
          report.data.rows
            .map(parseCellFor(name, _))
            .groupBy(_._1)
            .mapValues(buildScore)
        )
    }
  }

  def buildRequest(name: Option[CountryName], viewId: String, dateRange: DateRange): RequestBody =
    RequestBody(ReportRequest(
      viewId                 = viewId,
      dimensions             = dimensionsFor(name),
      metrics                = List(Metric.eventValue),
      dateRanges             = List(dateRange),
      dimensionFilterClauses = dimensionFiltersFor(name),
      orderBys               = {
        import order.OrderBy.{ category, eventValue }
        List(category, eventValue)
      }
    ))

  private[Converters] def dimensionsFor(code: Option[CountryName]): List[Dimension] = {
    val tailDimensions = List(Dimension.category, Dimension.packageName)
    code.fold(tailDimensions)(_ ⇒ Dimension.country :: tailDimensions)
  }

  private[Converters] def dimensionFiltersFor(code: Option[CountryName]): DimensionFilter.Clauses =
    code
      .map(name ⇒ singleClause(Filter.isCountry(name)))
      .getOrElse(Nil)

  private[Converters] def parseCellFor(name: Option[CountryName], row: ReportRow): Cell = {
    val tailDimensions: List[String] = name.fold(row.dimensions)(_ ⇒ row.dimensions.drop(1)) // drop first dimension: country

    val List(category, packageStr) = tailDimensions
    (category, Package(packageStr))
  }

}
