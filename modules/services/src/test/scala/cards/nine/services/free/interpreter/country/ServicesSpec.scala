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

import cards.nine.commons.NineCardsErrors.{ CountryNotFound, NineCardsError }
import cards.nine.services.free.domain.Country
import cards.nine.services.free.domain.Country.Queries
import cards.nine.services.persistence.NineCardsGenEntities.WrongIsoCode2
import cards.nine.services.persistence.{ DomainDatabaseContext, NineCardsScalacheckGen }
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ServicesSpec
  extends Specification
  with ScalaCheck
  with DomainDatabaseContext
  with NineCardsScalacheckGen {

  sequential

  "getCountryByIsoCode2" should {
    "return a CountryNotFound error if a non-existing ISO code is provided" in {
      prop { isoCode: WrongIsoCode2 ⇒
        val country = countryPersistenceServices.getCountryByIsoCode2(isoCode.value).transactAndRun
        val error = CountryNotFound(s"Country with ISO code2 ${isoCode.value} doesn't exist")

        country must beLeft[NineCardsError](error)
      }
    }

    "return a country if a valid ISO code is provided" in {
      prop { index: Int ⇒

        val (searchedCountry, country) = {
          for {
            countries ← getItems[Country](Queries.getAllSql)
            searchedCountry = countries(Math.abs(index % countries.size))
            country ← countryPersistenceServices.getCountryByIsoCode2(searchedCountry.isoCode2)
          } yield (searchedCountry, country)
        }.transactAndRun

        country must beRight[Country].which { c ⇒
          c.isoCode2 must_== searchedCountry.isoCode2
          c.isoCode3 must_== searchedCountry.isoCode3
          c.name must_== searchedCountry.name
          c.continent must_== searchedCountry.continent
        }
      }
    }
  }
}
