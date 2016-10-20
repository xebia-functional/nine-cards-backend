package cards.nine.services.free.domain

import cards.nine.domain.analytics.{ AnalyticsToken, DateRange }
import cards.nine.domain.application.{ Category, Package }
import cats.data.Xor

object Ranking {

  case class RankingParams(dateRange: DateRange, rankingLength: Int, auth: AnalyticsToken)

  case class Rankings(categories: Map[Category, CategoryRanking]) extends AnyVal

  case class CategoryRanking(ranking: List[Package]) extends AnyVal

  case class UpdateRankingSummary(created: Int, deleted: Int)

  case class RankingError(code: Int, message: String, status: String)

  type TryRanking = RankingError Xor Rankings

  case class GoogleAnalyticsRanking(categories: Map[String, List[Package]]) extends AnyVal

  case class AppRankingInfo(packageName: Package, position: Int)

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
