package cards.nine.services.free.interpreter.analytics

import cards.nine.domain.analytics.{ GeoScope ⇒ DomainScope, _ }
import cards.nine.domain.application.Category
import cards.nine.services.free.domain.PackageName
import cards.nine.services.free.domain.rankings._

object Converters {

  import model._

  type Cell = (Category, PackageName)

  def parseRanking(response: ResponseBody, rankingSize: Int, geoScope: DomainScope): Ranking = {
    def buildScore(cells: List[Cell]): CategoryRanking = CategoryRanking(
      cells.map(_._2).take(rankingSize)
    )
    val rows: List[ReportRow] = response.reports.headOption match {
      case Some(report) ⇒ report.data.rows
      case None ⇒ throw new RuntimeException("Response from Google API contained no report")
    }
    val scores: Map[Category, CategoryRanking] =
      rows
        .map(parseCellFor(geoScope, _))
        .groupBy(_._1)
        .mapValues(buildScore)
    Ranking(scores)
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
      case ContinentScope(_) ⇒ Dimension.continent :: tailDimensions
      case WorldScope ⇒ tailDimensions
    }
  }

  private[Converters] def dimensionFiltersFor(scope: DomainScope): DimensionFilter.Clauses = {
    import DimensionFilter._
    scope match {
      case CountryScope(country) ⇒ singleClause(Filter.isCountry(country))
      case ContinentScope(continent) ⇒ singleClause(Filter.isContinent(continent))
      case WorldScope ⇒ Nil
    }
  }

  private[Converters] def parseCellFor(scope: DomainScope, row: ReportRow): Cell = {
    def makeCell(categoryStr: String, packageStr: String): Cell =
      (Category.withName(categoryStr), PackageName(packageStr))

    scope match {
      case CountryScope(_) ⇒
        val List(_country, categoryStr, packageStr) = row.dimensions
        makeCell(categoryStr, packageStr)
      case ContinentScope(_) ⇒
        val List(_continent, categoryStr, packageStr) = row.dimensions
        makeCell(categoryStr, packageStr)
      case WorldScope ⇒
        val List(categoryStr, packageStr) = row.dimensions
        makeCell(categoryStr, packageStr)
    }
  }

}
