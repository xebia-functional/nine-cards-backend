package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.analytics.{ RankedApp, UnrankedApp }
import cards.nine.services.free.domain.Ranking._
import cats.free.{ :<:, Free }

object Ranking {

  sealed trait Ops[A]

  case class GetRanking(scope: GeoScope) extends Ops[GoogleAnalyticsRanking]

  case class GetRankingForApps(scope: GeoScope, apps: Set[UnrankedApp]) extends Ops[Result[List[RankedApp]]]

  case class UpdateRanking(scope: GeoScope, ranking: GoogleAnalyticsRanking) extends Ops[UpdateRankingSummary]

  class Services[F[_]](implicit I: Ops :<: F) {
    def getRanking(scope: GeoScope): Free[F, GoogleAnalyticsRanking] =
      Free.inject[Ops, F](GetRanking(scope))

    def getRankingForApps(scope: GeoScope, apps: Set[UnrankedApp]): NineCardsService[F, List[RankedApp]] =
      NineCardsService(Free.inject[Ops, F](GetRankingForApps(scope, apps)))

    def updateRanking(scope: GeoScope, ranking: GoogleAnalyticsRanking): Free[F, UpdateRankingSummary] =
      Free.inject[Ops, F](UpdateRanking(scope, ranking))
  }

  object Services {

    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] = new Services

  }

}
