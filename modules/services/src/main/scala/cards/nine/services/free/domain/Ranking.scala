package cards.nine.services.free.domain

import cards.nine.domain.analytics._
import cards.nine.domain.analytics.ReportType.AppsRankingByCategory
import cards.nine.domain.application.Package

object Ranking {

  case class GoogleAnalyticsRanking(categories: Map[String, List[Package]]) extends AnyVal

  case class CountriesWithRanking(countries: List[CountryIsoCode]) extends AnyVal

  case class UpdateRankingSummary(created: Int, deleted: Int)

  case class RankingError(code: Int, message: String, status: String)

  case class AppRankingInfo(packageName: Package, position: Int)

  case class CacheKey(scope: GeoScope, reportType: ReportType)

  case class CacheVal(ranking: Option[GoogleAnalyticsRanking])

  object CacheKey {

    def worldScope: CacheKey = CacheKey(WorldScope, AppsRankingByCategory)

    def countryScope(code: String): CacheKey = CacheKey(
      scope      = CountryScope(CountryIsoCode(code.toLowerCase)),
      reportType = AppsRankingByCategory
    )

  }
}
