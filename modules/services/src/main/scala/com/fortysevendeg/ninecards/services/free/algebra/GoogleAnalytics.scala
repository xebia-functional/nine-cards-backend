package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{ Free, Inject }
import com.fortysevendeg.ninecards.services.free.domain.rankings._

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