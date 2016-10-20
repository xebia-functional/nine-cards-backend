package cards.nine.services.free.domain

import cards.nine.domain.application.Package

object RedisRanking {

  case class GoogleAnalyticsRanking(categories: Map[String, List[Package]]) extends AnyVal

  case class AppRankingInfo(packageName: Package, position: Int)

  case class UpdateRankingSummary(created: Int, deleted: Int)

  sealed abstract class ReportType extends Product with Serializable

  case object AppsRankingByCategory extends ReportType

  sealed abstract class GeoScope extends Product with Serializable

  case class CountryScope(code: String) extends GeoScope

  case object WorldScope extends GeoScope

  case class CacheKey(scope: GeoScope, reportType: ReportType)

  case class CacheVal(ranking: Option[GoogleAnalyticsRanking])

  object CacheKey {

    def worldScope: CacheKey = CacheKey(WorldScope, AppsRankingByCategory)

    def countryScope(code: String): CacheKey = CacheKey(CountryScope(code), AppsRankingByCategory)

  }

}
