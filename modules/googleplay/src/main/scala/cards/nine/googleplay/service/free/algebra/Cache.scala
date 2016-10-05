package cards.nine.googleplay.service.free.algebra

import cats.free.{ Free, Inject }
import cards.nine.googleplay.domain.{ Package, FullCard }

object Cache {

  sealed trait Ops[A]

  case class GetValid(pack: Package) extends Ops[Option[FullCard]]

  case class PutResolved(card: FullCard) extends Ops[Unit]

  case class MarkPending(`package`: Package) extends Ops[Unit]

  case class UnmarkPending(`package`: Package) extends Ops[Unit]

  case class MarkError(`package`: Package) extends Ops[Unit]

  case class ClearInvalid(`package`: Package) extends Ops[Unit]

  case class IsPending(`package`: Package) extends Ops[Boolean]

  case class ListPending(limit: Int) extends Ops[List[Package]]

  class Service[F[_]](implicit I: Inject[Ops, F]) {

    def getValid(pack: Package): Free[F, Option[FullCard]] =
      Free.inject[Ops, F](GetValid(pack))

    def putResolved(card: FullCard): Free[F, Unit] =
      Free.inject[Ops, F](PutResolved(card))

    def markPending(pack: Package): Free[F, Unit] =
      Free.inject[Ops, F](MarkPending(pack))

    def unmarkPending(pack: Package): Free[F, Unit] =
      Free.inject[Ops, F](MarkPending(pack))

    def markError(pack: Package): Free[F, Unit] =
      Free.inject[Ops, F](MarkError(pack))

    def clearInvalid(pack: Package): Free[F, Unit] =
      Free.inject[Ops, F](ClearInvalid(pack))

    def isPending(pack: Package): Free[F, Boolean] =
      Free.inject[Ops, F](IsPending(pack))

    def listPending(limit: Int): Free[F, List[Package]] =
      Free.inject[Ops, F](ListPending(limit))

  }

  object Service {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Service[F] = new Service[F]
  }

}