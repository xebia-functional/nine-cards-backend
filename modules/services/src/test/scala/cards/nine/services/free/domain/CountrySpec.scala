package cards.nine.services.free.domain

import cards.nine.domain.analytics.{ Country ⇒ ACountry, _ }
import org.specs2.mutable.Specification

class CountrySpec extends Specification {

  val antarctica = Country("AQ", Option("ATA"), "Antarctica", "Antarctica")

  val unitedStates = Country("US", Option("USA"), "United States", "Americas")

  "toGeoScope" should {
    "return a CountryScope value if there is an EnumEntry for the country" in {

      val geoScope = unitedStates.toGeoScope

      geoScope must_== CountryScope(ACountry.United_States)
    }

    "return a WorldScope value if there aren't EnumEntry for the country neither the continent" in {

      val geoScope = antarctica.toGeoScope

      geoScope must_== WorldScope
    }
  }
}
