package cards.nine.services.free.domain.queries

import cards.nine.services.free.domain.Country.Queries._
import cards.nine.services.persistence.DomainDatabaseContext
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class CountryQueriesSpec
  extends Specification
  with AnalysisSpec
  with DomainDatabaseContext {

  val getAllQuery = countryPersistence.generateQuery(
    sql = getAllSql
  )
  check(getAllQuery)

  val getByIsoCode2Query = countryPersistence.generateQuery(
    sql    = getByIsoCode2Sql,
    values = "US"
  )
  check(getByIsoCode2Query)
}
