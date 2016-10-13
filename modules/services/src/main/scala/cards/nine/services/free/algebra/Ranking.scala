package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService.{ NineCardsService, Result }
import cards.nine.services.free.domain.rankings._
import cats.free.{ :<:, Free }

object Ranking {

  sealed trait Ops[A]

  case class GetRanking(scope: GeoScope) extends Ops[Ranking]

  case class GetRankingForApps(scope: GeoScope, apps: Set[UnrankedApp]) extends Ops[Result[List[RankedApp]]]

  case class UpdateRanking(scope: GeoScope, ranking: Ranking) extends Ops[UpdateRankingSummary]

  class Services[F[_]](implicit I: Ops :<: F) {

    def getRanking(scope: GeoScope): Free[F, Ranking] =
      Free.inject[Ops, F](GetRanking(scope))

    def getRankingForApps(scope: GeoScope, apps: Set[UnrankedApp]): NineCardsService[F, List[RankedApp]] =
      NineCardsService(Free.inject[Ops, F](GetRankingForApps(scope, apps)))

    def updateRanking(scope: GeoScope, ranking: Ranking): Free[F, UpdateRankingSummary] =
      Free.inject[Ops, F](UpdateRanking(scope, ranking))
  }

  object Services {

    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] = new Services

  }

}
