package cards.nine.services.free.interpreter.country

import cards.nine.services.free.algebra.Country._
import cards.nine.services.free.domain.Country
import cards.nine.services.free.domain.Country.Queries
import cards.nine.services.persistence.Persistence
import cats.~>
import doobie.imports._

class Services(persistence: Persistence[Country]) extends (Ops ~> ConnectionIO) {

  def getCountryByIsoCode2(isoCode: String): ConnectionIO[Option[Country]] =
    persistence.fetchOption(Queries.getByIsoCode2Sql, isoCode)

  def apply[A](fa: Ops[A]): ConnectionIO[A] = fa match {
    case GetCountryByIsoCode2(isoCode) ⇒ getCountryByIsoCode2(isoCode)
  }
}

object Services {

  def services(implicit persistence: Persistence[Country]) = new Services(persistence)
}
