package cards.nine.services.free.algebra

import cards.nine.services.free.domain
import cats.free.{ Free, Inject }

object Country {

  sealed trait Ops[A]

  case class GetCountryByIsoCode2(isoCode: String) extends Ops[Option[domain.Country]]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def getCountryByIsoCode2(isoCode: String): Free[F, Option[domain.Country]] =
      Free.inject[Ops, F](GetCountryByIsoCode2(isoCode))

  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] = new Services

  }

}
