package cards.nine.services.free.interpreter.country

import cards.nine.services.free.domain.Country
import cards.nine.services.free.domain.Country.Queries
import cards.nine.services.persistence.Persistence
import doobie.imports._

class Services(persistence: Persistence[Country]) {

  def getCountryByIsoCode2(isoCode: String): ConnectionIO[Option[Country]] =
    persistence.fetchOption(Queries.getByIsoCode2Sql, isoCode)
}

object Services {

  def services(implicit persistence: Persistence[Country]) = new Services(persistence)
}
