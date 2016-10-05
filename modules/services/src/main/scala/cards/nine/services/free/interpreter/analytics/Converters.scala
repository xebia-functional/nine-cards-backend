package cards.nine.services.free.interpreter.analytics

import cards.nine.services.free.domain.{ Category, PackageName }
import cards.nine.services.free.domain.rankings.{ GeoScope ⇒ DomainScope, _ }

object Converters {

  import model._

  type Cell = (Category, PackageName)

  def parseRanking(response: ResponseBody, rankingSize: Int, geoScope: DomainScope): Ranking = {
    val scope = GeoScope(geoScope)

    def buildScore(cells: List[Cell]): CategoryRanking = CategoryRanking(
      cells.map(_._2).take(rankingSize)
    )
    val rows: List[ReportRow] = response.reports.headOption match {
      case Some(report) ⇒ report.data.rows
      case None ⇒ throw new RuntimeException("Response from Google API contained no report")
    }
    val scores: Map[Category, CategoryRanking] =
      rows
        .map(scope.parseCell)
        .groupBy(_._1)
        .mapValues(buildScore)
    Ranking(scores)
  }

  def buildRequest(geoScope: DomainScope, viewId: String, dateRange: DateRange): RequestBody = {
    val scope = Converters.GeoScope(geoScope)

    RequestBody(ReportRequest(
      viewId                 = viewId,
      dimensions             = scope.dimensions,
      metrics                = List(Metric.eventValue),
      dateRanges             = List(dateRange),
      dimensionFilterClauses = scope.dimensionFilters,
      orderBys               = {
        import order.OrderBy.{ category, eventValue }
        List(category, eventValue)
      }
    ))
  }

  private[Converters] sealed trait GeoScope {
    def dimensions: List[Dimension]
    def dimensionFilters: DimensionFilter.Clauses
    def parseCell(row: ReportRow): Cell
  }

  private[Converters] object GeoScope {
    def apply(scope: DomainScope): GeoScope = scope match {
      case CountryScope(country) ⇒ new Converters.GeoCountry(country)
      case ContinentScope(continent) ⇒ new Converters.GeoContinent(continent)
      case WorldScope ⇒ Converters.GeoWorld
    }
  }

  private[Converters] class GeoCountry(country: Country) extends GeoScope {
    override val dimensions = List(Dimension.country, Dimension.category, Dimension.packageName)
    override val dimensionFilters = DimensionFilter.singleClause(DimensionFilter.Filter.isCountry(country))
    def parseCell(row: ReportRow): Cell = {
      val List(_country, categoryStr, packageStr) = row.dimensions
      makeCell(categoryStr, packageStr)
    }
  }

  private[Converters] class GeoContinent(continent: Continent) extends GeoScope {
    override val dimensions = List(Dimension.continent, Dimension.category, Dimension.packageName)
    override val dimensionFilters =
      DimensionFilter.singleClause(DimensionFilter.Filter.isContinent(continent))
    def parseCell(row: ReportRow): Cell = {
      val List(_continent, categoryStr, packageStr) = row.dimensions
      makeCell(categoryStr, packageStr)
    }
  }

  private[Converters] object GeoWorld extends GeoScope {
    override val dimensions = List(Dimension.category, Dimension.packageName)
    override val dimensionFilters = List()
    def parseCell(row: ReportRow): Cell = {
      val List(categoryStr, packageStr) = row.dimensions
      makeCell(categoryStr, packageStr)
    }
  }

  private[this] def makeCell(categoryStr: String, packageStr: String): Cell =
    Category.withName(categoryStr) → PackageName(packageStr)

}
