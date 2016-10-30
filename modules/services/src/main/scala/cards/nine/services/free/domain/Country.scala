package cards.nine.services.free.domain

case class Country(
  isoCode2: String,
  isoCode3: Option[String],
  name: String,
  continent: String
)

object Country {

  object Queries {
    val getAllSql = "select * from countries"
    val getCountriesWithPaginationSql = "select * from countries order by iso2 limit ? offset ?"
    val getByIsoCode2Sql = "select * from countries where iso2=?"
  }

}
