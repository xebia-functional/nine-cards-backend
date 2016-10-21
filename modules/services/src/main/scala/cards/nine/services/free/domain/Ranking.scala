package cards.nine.services.free.domain

import cards.nine.domain.analytics.ReportType
import cards.nine.domain.analytics.ReportType.AppsRankingByCategory
import cards.nine.domain.application.Package
import cats.data.Xor

object Ranking {

  case class GoogleAnalyticsRanking(categories: Map[String, List[Package]]) extends AnyVal

  type TryRanking = RankingError Xor GoogleAnalyticsRanking

  case class UpdateRankingSummary(created: Int, deleted: Int)

  case class RankingError(code: Int, message: String, status: String)

  case class AppRankingInfo(packageName: Package, position: Int)

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
