package cards.nine.services.free.domain

import org.specs2.mutable.Specification

class CountrySpec extends Specification {

  val antarctica = Country("AQ", Option("ATA"), "Antarctica", "Antarctica")

  val italy = Country("IT", Option("ITA"), "Italy", "Europe")

  val unitedStates = Country("US", Option("USA"), "United States", "Americas")

  "toGeoScope" should {
    "return a CountryScope value if there is an EnumEntry for the country" in {

      val geoScope = unitedStates.toGeoScope

      geoScope must_== rankings.CountryScope(rankings.Country.United_States)
    }

    "it should return a ContinentScope value if there isn't an EnumEntry for the country, but " +
      "does exist for the continent of the country" in {

        val geoScope = italy.toGeoScope

        geoScope must_== rankings.ContinentScope(rankings.Continent.Europe)
      }

    "return a WorldScope value if there aren't EnumEntry for the country neither the continent" in {

      val geoScope = antarctica.toGeoScope

      geoScope must_== rankings.WorldScope
    }
  }
}
