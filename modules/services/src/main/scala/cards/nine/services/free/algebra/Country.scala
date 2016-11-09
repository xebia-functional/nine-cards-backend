package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.pagination.Page
import cards.nine.services.free.domain
import cats.free.{ :<:, Free }

object Country {

  sealed trait Ops[A]

  case class GetCountries(pageParams: Page) extends Ops[Result[List[domain.Country]]]

  case class GetCountryByIsoCode2(isoCode: String) extends Ops[Result[domain.Country]]

  class Services[F[_]](implicit I: Ops :<: F) {

    def getCountries(pageParams: Page): NineCardsService[F, List[domain.Country]] =
      NineCardsService(Free.inject[Ops, F](GetCountries(pageParams)))

    def getCountryByIsoCode2(isoCode: String): NineCardsService[F, domain.Country] =
      NineCardsService(Free.inject[Ops, F](GetCountryByIsoCode2(isoCode)))
  }

  object Services {

    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] = new Services

  }

}
