package cards.nine.services.free.interpreter.ranking

import cards.nine.commons.NineCardsErrors.RankingNotFound
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.redis._
import cards.nine.domain.analytics._
import cards.nine.domain.application.{ Moment, Widget }
import cards.nine.services.free.algebra.Ranking._
import cards.nine.services.free.domain.Ranking._
import cats.instances.list._
import cats.instances.map._
import cats.syntax.cartesian._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.semigroup._
import cats.~>
import scala.concurrent.ExecutionContext

class Services(implicit ec: ExecutionContext) extends (Ops ~> RedisOps) {

  import Coders._
  import RedisOps._

  private[this] val wrap = new CacheWrapper[CacheKey, CacheVal]

  private[this] def getForScope(scope: GeoScope): RedisOps[Option[GoogleAnalyticsRanking]] = {
    def generateCacheKey(scope: GeoScope) = scope match {
      case WorldScope ⇒ CacheKey.worldScope
      case CountryScope(code) ⇒ CacheKey.countryScope(code.value)
    }
    wrap.get(generateCacheKey(scope)).map(_.flatMap(_.ranking))
  }

  private[this] def generateRanking(scope: GeoScope): RedisOps[GoogleAnalyticsRanking] = {
    def getRankingByScope(scope: GeoScope): RedisOps[GoogleAnalyticsRanking] =
      getForScope(scope).map(_.getOrElse(GoogleAnalyticsRanking(Map.empty)))

    def fillRanking(country: GoogleAnalyticsRanking, world: GoogleAnalyticsRanking): GoogleAnalyticsRanking =
      GoogleAnalyticsRanking(country.categories.combine(world.categories).mapValues(_.distinct))

    scope match {
      case WorldScope ⇒
        getRankingByScope(scope)
      case CountryScope(_) ⇒
        val country = getRankingByScope(scope)
        val world = getRankingByScope(WorldScope)
        (country |@| world) map fillRanking
    }
  }

  def apply[A](fa: Ops[A]): RedisOps[A] = fa match {

    case GetRanking(scope) ⇒
      def fromValue(ranking: Option[GoogleAnalyticsRanking]): Result[GoogleAnalyticsRanking] = {
        lazy val error = RankingNotFound(s"Ranking not found for $scope")
        Either.fromOption(ranking, error)
      }
      getForScope(scope).map(fromValue)

    case GetRankingForApps(scope, apps) ⇒

      def getForApps(rankings: GoogleAnalyticsRanking): Result[List[RankedApp]] = {
        val rankingsByCategory = rankings.categories.filterKeys(c ⇒ !Moment.isMoment(c))
        val packagesByCategory = apps.toList.groupBy(_.category).mapValues(_.map(_.packageName))
        val rankedByCategory = rankingsByCategory flatMap {
          case (category, ranking) ⇒
            ranking
              .intersect(packagesByCategory.getOrElse(category, Nil))
              .zipWithIndex
              .map { case (pack, position) ⇒ RankedApp(pack, category, Option(position)) }
        }
        Either.right(rankedByCategory.toList)
      }

      generateRanking(scope).map(getForApps)

    case GetRankingForAppsWithinMoments(scope, apps, moments) ⇒
      def getForAppsWithinMoments(rankings: GoogleAnalyticsRanking): Result[List[RankedApp]] = {
        val rankingsByMoment = rankings.categories.filterKeys(moment ⇒ moments.contains(moment))

        val rankedByMoment = rankingsByMoment flatMap {
          case (category, ranking) ⇒
            ranking
              .intersect(apps)
              .zipWithIndex
              .map { case (pack, position) ⇒ RankedApp(pack, category, Option(position)) }
        }

        Either.right(rankedByMoment.toList)
      }

      generateRanking(scope).map(getForAppsWithinMoments)

    case GetRankingForWidgets(scope, apps, moments) ⇒
      def getForWidgets(rankings: GoogleAnalyticsRanking): Result[List[RankedWidget]] = {
        val rankingsByMoment =
          rankings
            .categories
            .filterKeys(moment ⇒ moments.contains(moment))
            .mapValues(packages ⇒ packages flatMap (p ⇒ Widget(p.value)))

        val rankedByMoment = rankingsByMoment flatMap {
          case (moment, ranking) ⇒
            ranking
              .filter(w ⇒ apps.contains(w.packageName))
              .zipWithIndex
              .map { case (widget, position) ⇒ RankedWidget(widget, moment, Option(position)) }
        }

        Either.right(rankedByMoment.toList)
      }

      generateRanking(scope).map(getForWidgets)

    case UpdateRanking(scope, ranking) ⇒
      val (key, countryCode) = scope match {
        case WorldScope ⇒
          (CacheKey.worldScope, None)
        case CountryScope(code) ⇒
          (CacheKey.countryScope(code.value), Option(code))
      }
      val value = CacheVal(Option(ranking))
      val summary = UpdateRankingSummary(countryCode, ranking.categories.values.size)

      wrap.put((key, value)).map(_ ⇒ Either.right(summary))
  }
}

object Services {

  def services(implicit ec: ExecutionContext) = new Services()

}
