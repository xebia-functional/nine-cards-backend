package cards.nine.services.free.domain

import cards.nine.services.free.domain.rankings.{ GeoScopeStringOps, WorldScope }
import enumeratum.Enum

case class Country(
  id: Long,
  isoCode2: String,
  isoCode3: Option[String],
  name: String,
  continent: String
)

object Country {

  implicit class CountryOps(country: Country) {
    def toGeoScope(implicit countryEnum: Enum[rankings.Country], continentEnum: Enum[rankings.Continent]) =
      country.name.replace(" ", "_").toOptionalCountry
        .getOrElse(
          country.continent.toOptionalContinent
            .getOrElse(WorldScope)
        )
  }

  object Queries {
    val getAllSql = "select * from countries"
    val getByIsoCode2Sql = "select * from countries where iso2=?"
  }

}
