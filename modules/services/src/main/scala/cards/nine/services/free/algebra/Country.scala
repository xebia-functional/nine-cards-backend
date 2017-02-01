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
import cards.nine.domain.pagination.Page
import cards.nine.services.free.domain
import cats.free.{ :<:, Free }

object Country {

  sealed trait Ops[A]

  case class GetCountries(pageParams: Page) extends Ops[Result[List[domain.Country]]]

  case class GetCountryByIsoCode2(isoCode: String) extends Ops[Result[domain.Country]]

  class Services[F[_]](implicit I: Ops :<: F) {

    def getCountries(pageParams: Page): NineCardsService[F, List[domain.Country]] =
      NineCardsService(Free.inject[Ops, F](GetCountries(pageParams)))

    def getCountryByIsoCode2(isoCode: String): NineCardsService[F, domain.Country] =
      NineCardsService(Free.inject[Ops, F](GetCountryByIsoCode2(isoCode)))
  }

  object Services {

    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] = new Services

  }

}
