package cards.nine.services.free.domain

import cards.nine.domain.analytics.{ CountryScope, ContinentScope, GeoScope, WorldScope }

case class Country(
  isoCode2: String,
  isoCode3: Option[String],
  name: String,
  continent: String
)

object Country {

  implicit class CountryOps(country: Country) {
    def toGeoScope: GeoScope =
      CountryScope.lookup(country.name.replace(" ", "_"))
        .orElse(ContinentScope.lookup(country.continent))
        .getOrElse(WorldScope)
  }

  object Queries {
    val getAllSql = "select * from countries"
    val getByIsoCode2Sql = "select * from countries where iso2=?"
  }

}
