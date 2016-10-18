package cards.nine.domain.analytics

import org.specs2.mutable.Specification

class GeoScopeSpec extends Specification {

  "GeoScopeStringOps.toOptionalContinent" should {
    "return None if there is no EnumEntry for the given continent" in {
      ContinentScope.lookup("Antarctica") must beNone
    }

    "return a ContinentScope value if there is a EnumEntry for the given continent" in {
      ContinentScope.lookup("Americas") must beSome[ContinentScope].which { scope ⇒
        scope.continent must_== Continent.Americas
      }
    }
  }

  "GeoScopeStringOps.toOptionalCountry" should {
    "return None if there is no EnumEntry for the given country" in {
      CountryScope.lookup("Italy") must beNone
    }

    "return a CountryScope value if there is a EnumEntry for the given country" in {
      CountryScope.lookup("Spain") must beSome[CountryScope].which { scope ⇒
        scope.country must_== Country.Spain
      }
    }
  }
}
