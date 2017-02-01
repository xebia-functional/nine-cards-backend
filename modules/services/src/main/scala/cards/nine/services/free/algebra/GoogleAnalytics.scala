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