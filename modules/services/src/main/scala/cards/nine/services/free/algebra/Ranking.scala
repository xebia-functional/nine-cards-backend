package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.analytics.{ GeoScope, RankedApp, RankedWidget, UnrankedApp }
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
