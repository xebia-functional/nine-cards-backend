/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.services.free.interpreter.ranking

import cards.nine.commons.NineCardsErrors.RankingNotFound
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.redis._
import cards.nine.domain.analytics._
import cards.nine.domain.application.{ Moment, Package, Widget }
import cards.nine.services.free.algebra.RankingS._
import cards.nine.services.free.domain.Ranking._
import cats.instances.list._
import cats.instances.map._
import cats.syntax.cartesian._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.semigroup._
import scala.concurrent.ExecutionContext

class Services(implicit ec: ExecutionContext) extends Handler[RedisOps] {

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

  def getRanking(scope: GeoScope): RedisOps[Result[GoogleAnalyticsRanking]] = {
    def fromValue(ranking: Option[GoogleAnalyticsRanking]): Result[GoogleAnalyticsRanking] = {
      lazy val error = RankingNotFound(s"Ranking not found for $scope")
      Either.fromOption(ranking, error)
    }
    getForScope(scope).map(fromValue)
  }

  override def updateRanking(
    scope: GeoScope, ranking: GoogleAnalyticsRanking
  ): RedisOps[Result[UpdateRankingSummary]] = {
    val (key, countryCode) = scope match {
      case WorldScope ⇒ (CacheKey.worldScope, None)
      case CountryScope(code) ⇒ (CacheKey.countryScope(code.value), Option(code))
    }
    val value = CacheVal(Option(ranking))
    val summary = UpdateRankingSummary(countryCode, ranking.categories.values.size)

    wrap.put((key, value)).map(_ ⇒ Either.right(summary))
  }

  override def rankApps(scope: GeoScope, apps: Set[UnrankedApp]): RedisOps[Result[List[RankedApp]]] = {
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
      Right(rankedByCategory.toList)
    }
    generateRanking(scope).map(getForApps)
  }

  override def rankAppsWithinMoments(
    scope: GeoScope,
    apps: List[Package],
    moments: List[String]
  ): RedisOps[Result[List[RankedApp]]] = {
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
  }

  protected[this] def rankWidgets(
    scope: GeoScope,
    apps: List[Package],
    moments: List[String]
  ): RedisOps[Result[List[RankedWidget]]] = {

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
  }

}

object Services {

  def services(implicit ec: ExecutionContext) = new Services()

}
