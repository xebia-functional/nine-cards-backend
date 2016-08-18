package com.fortysevendeg.ninecards.services.free.interpreter.analytics

import com.fortysevendeg.ninecards.services.free.domain.{ Category, PackageName }
import com.fortysevendeg.ninecards.services.free.domain.rankings.{ GeoScope ⇒ DomainScope, _ }
import scala.math.Ordering

object Converters {

  import model._

  type Cell = (Category, (PackageName, Int))

  def parseRanking(response: ResponseBody, rankingSize: Int, geoScope: DomainScope): Ranking = {
    val scope = GeoScope(geoScope)

    def buildScore(cells: List[Cell]): CategoryScore = {
      val rankingOrder: Ordering[(PackageName, Int)] = Ordering.Int.reverse.on(_._2)
      val map = cells
        .map(_._2)
        .sorted(rankingOrder)
        .take(rankingSize)
        .toMap
      CategoryScore(map)
    }
    val scores: Map[Category, CategoryScore] = response.reports.head.data.rows
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

  def checkResponse(scope: GeoScope, response: ResponseBody): Unit = {
    val expectedHeader = ColumnHeader(
      dimensions   = scope.dimensions.map(_.name),
      metricHeader = MetricHeader(List(MetricHeaderEntry.eventValue))
    )
    assert(response.reports.size == 1)
    val report: Report = response.reports.head
    assert(report.columnHeader == expectedHeader)
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
    override val dimensionFilters = DimensionFilter.singleton(DimensionFilter.Filter.isCountry(country))
    def parseCell(row: ReportRow): Cell = {
      val List(_country, categoryStr, packageStr) = row.dimensions
      val List(DateRangeValues(List(value))) = row.metrics
      makeCell(categoryStr, packageStr, value)
    }
  }

  private[Converters] class GeoContinent(continent: Continent) extends GeoScope {
    override val dimensions = List(Dimension.continent, Dimension.category, Dimension.packageName)
    override val dimensionFilters = DimensionFilter.singleton(DimensionFilter.Filter.isContinent(continent))
    def parseCell(row: ReportRow): Cell = {
      val List(_continent, categoryStr, packageStr) = row.dimensions
      val List(DateRangeValues(List(value))) = row.metrics
      makeCell(categoryStr, packageStr, value)
    }
  }

  private[Converters] object GeoWorld extends GeoScope {
    override val dimensions = List(Dimension.category, Dimension.packageName)
    override val dimensionFilters = List()
    def parseCell(row: ReportRow): Cell = {
      val List(categoryStr, packageStr) = row.dimensions
      val List(DateRangeValues(List(value))) = row.metrics
      makeCell(categoryStr, packageStr, value)
    }
  }

  private[this] def makeCell(categoryStr: String, packageStr: String, value: String) = {
    val category = Category.withName(categoryStr)
    val appPackage = PackageName(packageStr)
    category → (appPackage → value.toInt)
  }

}
