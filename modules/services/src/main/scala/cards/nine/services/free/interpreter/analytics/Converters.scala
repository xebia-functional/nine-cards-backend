package cards.nine.services.free.interpreter.analytics

import cards.nine.domain.analytics.{ GeoScope ⇒ DomainScope, _ }
import cards.nine.domain.application.Package
import cards.nine.services.free.domain.Ranking.GoogleAnalyticsRanking

object Converters {

  import model._

  type Cell = (String, Package)

  def parseRanking(response: ResponseBody, rankingSize: Int, geoScope: DomainScope): GoogleAnalyticsRanking = {
    def buildScore(cells: List[Cell]): List[Package] = cells.map(_._2).take(rankingSize)

    val rows: List[ReportRow] = response.reports.headOption match {
      case Some(report) ⇒ report.data.rows
      case None ⇒ throw new RuntimeException("Response from Google API contained no report")
    }
    val scores: Map[String, List[Package]] =
      rows
        .map(parseCellFor(geoScope, _))
        .collect { case (Some(cat), pack) ⇒ (cat, pack) }
        .groupBy(_._1)
        .mapValues(buildScore)
    GoogleAnalyticsRanking(scores)
  }

  def buildRequest(geoScope: DomainScope, viewId: String, dateRange: DateRange): RequestBody =
    RequestBody(ReportRequest(
      viewId                 = viewId,
      dimensions             = dimensionsFor(geoScope),
      metrics                = List(Metric.eventValue),
      dateRanges             = List(dateRange),
      dimensionFilterClauses = dimensionFiltersFor(geoScope),
      orderBys               = {
        import order.OrderBy.{ category, eventValue }
        List(category, eventValue)
      }
    ))

  private[Converters] def dimensionsFor(scope: DomainScope): List[Dimension] = {
    val tailDimensions = List(Dimension.category, Dimension.packageName)
    scope match {
      case CountryScope(_) ⇒ Dimension.country :: tailDimensions
      case WorldScope ⇒ tailDimensions
    }
  }

  private[Converters] def dimensionFiltersFor(scope: DomainScope): DimensionFilter.Clauses = {
    import DimensionFilter._
    scope match {
      case CountryScope(country) ⇒ singleClause(Filter.isCountry(country))
      case WorldScope ⇒ Nil
    }
  }

  private[Converters] def parseCellFor(scope: DomainScope, row: ReportRow) = {
    val tailDimensions: List[String] = scope match {
      case CountryScope(_) ⇒ row.dimensions.drop(1) // drop first dimension: country
      case WorldScope ⇒ row.dimensions
    }
    val List(category, packageStr) = tailDimensions
    (category, Package(packageStr))
  }

}
