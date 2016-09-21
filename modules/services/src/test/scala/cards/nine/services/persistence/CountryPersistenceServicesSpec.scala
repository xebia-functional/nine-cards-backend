package cards.nine.services.persistence

import cards.nine.services.free.domain.Country
import cards.nine.services.free.domain.Country._
import cards.nine.services.persistence.NineCardsGenEntities._
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach

class CountryPersistenceServicesSpec
  extends Specification
  with BeforeEach
  with ScalaCheck
  with DomainDatabaseContext
  with DisjunctionMatchers
  with NineCardsScalacheckGen {

  sequential

  def before = {
    flywaydb.clean()
    flywaydb.migrate()
  }

  "getCountryByIsoCode2" should {
    "return None if a non-existing ISO code is provided" in {
      prop { isoCode: WrongIsoCode2 ⇒
        val country = countryPersistenceServices.getCountryByIsoCode2(isoCode.value).transactAndRun

        country must beNone
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

        country must beSome[Country].which { c ⇒
          c.isoCode2 must_== searchedCountry.isoCode2
          c.isoCode3 must_== searchedCountry.isoCode3
          c.name must_== searchedCountry.name
          c.continent must_== searchedCountry.continent
        }
      }
    }
  }
}
