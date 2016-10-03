package cards.nine.googleplay.service.free.algebra

import cats.data.Xor
import cats.free.{ Free, Inject }
import cards.nine.googleplay.domain.{ FullCard, Package }
import cards.nine.googleplay.domain.webscrapper._

object webscrapper {

  sealed trait Ops[A]

  case class ExistsApp(pack: Package) extends Ops[Boolean]

  case class GetDetails(pack: Package) extends Ops[Failure Xor FullCard]

  class Service[F[_]](implicit i: Inject[Ops, F]) {

    def existsApp(pack: Package): Free[F, Boolean] =
      Free.inject[Ops, F](ExistsApp(pack))

    def getDetails(pack: Package): Free[F, Failure Xor FullCard] =
      Free.inject[Ops, F](GetDetails(pack))
  }

  object Service {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Service[F] = new Service[F]
  }

}
