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

    def getCountryRanking(country: Country, params: RankingParams): Free[F, TryRanking] =
      Free.inject[Ops, F](GetRanking(CountryScope(country), params))

    def getContinentalRanking(continent: Continent, params: RankingParams): Free[F, TryRanking] =
      Free.inject[Ops, F](GetRanking(ContinentScope(continent), params))

    def getWorldRanking(params: RankingParams): Free[F, TryRanking] =
      Free.inject[Ops, F](GetRanking(WorldScope, params))
  }

}