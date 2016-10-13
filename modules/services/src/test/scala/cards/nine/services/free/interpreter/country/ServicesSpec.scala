package cards.nine.services.free.interpreter.country

import cards.nine.commons.NineCardsErrors.{ CountryNotFound, NineCardsError }
import cards.nine.services.free.domain.Country
import cards.nine.services.free.domain.Country.Queries
import cards.nine.services.persistence.NineCardsGenEntities.WrongIsoCode2
import cards.nine.services.persistence.{ DomainDatabaseContext, NineCardsScalacheckGen }
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach

class ServicesSpec
  extends Specification
  with BeforeEach
  with ScalaCheck
  with DomainDatabaseContext
  with NineCardsScalacheckGen {

  sequential

  def before = {
    flywaydb.clean()
    flywaydb.migrate()
  }

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
