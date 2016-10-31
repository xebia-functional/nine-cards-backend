package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.analytics.{ CountryName, RankingParams }
import cards.nine.services.free.domain.Ranking.GoogleAnalyticsRanking
import cats.free.{ Free, Inject }

object GoogleAnalytics {

  sealed trait Ops[A]

  case class GetRanking(
    name: Option[CountryName],
    rankingParams: RankingParams
  ) extends Ops[Result[GoogleAnalyticsRanking]]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def getRanking(name: Option[CountryName], params: RankingParams): NineCardsService[F, GoogleAnalyticsRanking] =
      NineCardsService(Free.inject[Ops, F](GetRanking(name, params)))
  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] =
      new Services

  }

}