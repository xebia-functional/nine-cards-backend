package cards.nine.services.free.algebra

import cats.free.{ Free, Inject }
import cards.nine.domain.analytics.{ GeoScope, RankingParams }
import cards.nine.services.free.domain.Ranking.TryRanking

object GoogleAnalytics {

  sealed trait Ops[A]

  case class GetRanking(
    geoScope: GeoScope,
    rankingParams: RankingParams
  ) extends Ops[TryRanking]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def getRanking(scope: GeoScope, params: RankingParams): Free[F, TryRanking] =
      Free.inject[Ops, F](GetRanking(scope, params))

  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] =
      new Services

  }

}