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
package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.analytics._
import cards.nine.domain.application.Package
import cards.nine.services.free.domain.Ranking._
import cats.free.{ :<:, Free }

object Ranking {

  sealed trait Ops[A]

  case class GetRanking(scope: GeoScope) extends Ops[Result[GoogleAnalyticsRanking]]

  case class GetRankingForApps(scope: GeoScope, apps: Set[UnrankedApp]) extends Ops[Result[List[RankedApp]]]

  case class GetRankingForAppsWithinMoments(
    scope: GeoScope,
    apps: List[Package],
    moments: List[String]
  ) extends Ops[Result[List[RankedApp]]]

  case class GetRankingForWidgets(
    scope: GeoScope,
    apps: List[Package],
    moments: List[String]
  ) extends Ops[Result[List[RankedWidget]]]

  case class UpdateRanking(scope: GeoScope, ranking: GoogleAnalyticsRanking) extends Ops[Result[UpdateRankingSummary]]

  class Services[F[_]](implicit I: Ops :<: F) {
    def getRanking(scope: GeoScope): NineCardsService[F, GoogleAnalyticsRanking] =
      NineCardsService(Free.inject[Ops, F](GetRanking(scope)))

    def getRankingForApps(scope: GeoScope, apps: Set[UnrankedApp]): NineCardsService[F, List[RankedApp]] =
      NineCardsService(Free.inject[Ops, F](GetRankingForApps(scope, apps)))

    def getRankingForAppsWithinMoments(
      scope: GeoScope,
      apps: List[Package],
      moments: List[String]
    ): NineCardsService[F, List[RankedApp]] =
      NineCardsService(Free.inject[Ops, F](GetRankingForAppsWithinMoments(scope, apps, moments)))

    def getRankingForWidgets(
      scope: GeoScope,
      apps: List[Package],
      moments: List[String]
    ): NineCardsService[F, List[RankedWidget]] =
      NineCardsService(Free.inject[Ops, F](GetRankingForWidgets(scope, apps, moments)))

    def updateRanking(scope: GeoScope, ranking: GoogleAnalyticsRanking): NineCardsService[F, UpdateRankingSummary] =
      NineCardsService(Free.inject[Ops, F](UpdateRanking(scope, ranking)))
  }

  object Services {

    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] = new Services

  }

}
