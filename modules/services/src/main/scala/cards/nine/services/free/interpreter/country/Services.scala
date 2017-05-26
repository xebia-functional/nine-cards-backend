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
package cards.nine.services.free.interpreter.country

import cards.nine.commons.NineCardsErrors.CountryNotFound
import cards.nine.domain.pagination.Page
import cards.nine.services.common.PersistenceService
import cards.nine.services.common.PersistenceService._
import cards.nine.services.free.algebra.CountryR._
import cards.nine.services.free.domain.Country
import cards.nine.services.free.domain.Country.Queries
import cards.nine.services.persistence.Persistence
import cats.syntax.either._
import doobie.imports.ConnectionIO

class Services(persistence: Persistence[Country]) extends Handler[ConnectionIO] {

  def getCountries(pageParams: Page): PersistenceService[List[Country]] =
    PersistenceService {
      persistence.fetchList(
        sql    = Queries.getCountriesWithPaginationSql,
        values = (pageParams.pageSize, pageParams.pageNumber)
      )
    }

  def getCountryByIsoCode2(isoCode: String): PersistenceService[Country] =
    persistence.fetchOption(Queries.getByIsoCode2Sql, isoCode.toUpperCase) map {
      Either.fromOption(_, CountryNotFound(s"Country with ISO code2 $isoCode doesn't exist"))
    }

}

object Services {

  def services(implicit persistence: Persistence[Country]) = new Services(persistence)
}
