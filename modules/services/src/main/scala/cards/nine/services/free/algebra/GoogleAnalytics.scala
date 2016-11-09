package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.analytics.{ CountryIsoCode, RankingParams }
import cards.nine.services.free.domain.Ranking.{ CountriesWithRanking, GoogleAnalyticsRanking }
import cats.free.{ Free, Inject }

object GoogleAnalytics {

  sealed trait Ops[A]

  case class GetRanking(
    code: Option[CountryIsoCode],
    categories: List[String],
    rankingParams: RankingParams
  ) extends Ops[Result[GoogleAnalyticsRanking]]

  case class GetCountriesWithRanking(
    rankingParams: RankingParams
  ) extends Ops[Result[CountriesWithRanking]]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def getCountriesWithRanking(rankingParams: RankingParams): NineCardsService[F, CountriesWithRanking] =
      NineCardsService(Free.inject[Ops, F](GetCountriesWithRanking(rankingParams)))

    def getRanking(
      code: Option[CountryIsoCode],
      categories: List[String],
      params: RankingParams
    ): NineCardsService[F, GoogleAnalyticsRanking] =
      NineCardsService(Free.inject[Ops, F](GetRanking(code, categories, params)))
  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] =
      new Services

  }

}