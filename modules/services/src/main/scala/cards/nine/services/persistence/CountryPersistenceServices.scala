package cards.nine.services.persistence

import cards.nine.services.free.domain.Country
import cards.nine.services.free.domain.Country.Queries
import doobie.imports.ConnectionIO

class CountryPersistenceServices(implicit countryPersistence: Persistence[Country]) {

  def getCountryByIsoCode2(isoCode: String): ConnectionIO[Option[Country]] =
    countryPersistence.fetchOption(Queries.getByIsoCode2Sql, isoCode)
}

object CountryPersistenceServices {

  implicit def persistenceServices(
    implicit
    countryPersistence: Persistence[Country]
  ) = new CountryPersistenceServices
}
