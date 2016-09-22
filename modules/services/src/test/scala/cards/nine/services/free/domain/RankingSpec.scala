package cards.nine.services.free.domain

import org.specs2.mutable.Specification
import cards.nine.services.free.domain.rankings._

class RankingSpec extends Specification {

  "GeoScopeStringOps.toOptionalContinent" should {
    "return None if there is no EnumEntry for the given continent" in {
      "Antarctica".toOptionalContinent must beNone
    }

    "return a ContinentScope value if there is a EnumEntry for the given continent" in {
      "Americas".toOptionalContinent must beSome[ContinentScope].which { scope ⇒
        scope.continent must_== Continent.Americas
      }
    }
  }

  "GeoScopeStringOps.toOptionalCountry" should {
    "return None if there is no EnumEntry for the given country" in {
      "Italy".toOptionalCountry must beNone
    }

    "return a CountryScope value if there is a EnumEntry for the given country" in {
      "Spain".toOptionalCountry must beSome[CountryScope].which { scope ⇒
        scope.country must_== rankings.Country.Spain
      }
    }
  }
}
