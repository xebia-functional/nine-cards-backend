package cards.nine.googleplay.service.free.algebra

import cats.free.{ Free, Inject }
import cards.nine.domain.application.{ FullCard, Package }

object Cache {

  sealed trait Ops[A]

  case class GetValid(pack: Package) extends Ops[Option[FullCard]]

  case class GetValidMany(packages: List[Package]) extends Ops[List[FullCard]]

  case class PutResolved(card: FullCard) extends Ops[Unit]

  case class PutResolvedMany(cards: List[FullCard]) extends Ops[Unit]

  case class PutPermanent(card: FullCard) extends Ops[Unit]

  case class MarkPending(`package`: Package) extends Ops[Unit]

  case class MarkPendingMany(packages: List[Package]) extends Ops[Unit]

  case class UnmarkPending(`package`: Package) extends Ops[Unit]

  case class UnmarkPendingMany(packages: List[Package]) extends Ops[Unit]

  case class MarkError(`package`: Package) extends Ops[Unit]

  case class MarkErrorMany(packages: List[Package]) extends Ops[Unit]

  case class ClearInvalid(`package`: Package) extends Ops[Unit]

  case class ClearInvalidMany(packages: List[Package]) extends Ops[Unit]

  case class IsPending(`package`: Package) extends Ops[Boolean]

  case class ListPending(limit: Int) extends Ops[List[Package]]

  class Service[F[_]](implicit I: Inject[Ops, F]) {

    def getValid(pack: Package): Free[F, Option[FullCard]] = Free.inject[Ops, F](GetValid(pack))

    def getValidMany(packages: List[Package]): Free[F, List[FullCard]] =
      Free.inject[Ops, F](GetValidMany(packages))

    def putResolved(card: FullCard): Free[F, Unit] = Free.inject[Ops, F](PutResolved(card))

    def putResolvedMany(cards: List[FullCard]): Free[F, Unit] =
      Free.inject[Ops, F](PutResolvedMany(cards))

    def putPermanent(card: FullCard): Free[F, Unit] = Free.inject[Ops, F](PutPermanent(card))

    def markPending(pack: Package): Free[F, Unit] = Free.inject[Ops, F](MarkPending(pack))

    def markPendingMany(packages: List[Package]): Free[F, Unit] =
      Free.inject[Ops, F](MarkPendingMany(packages))

    def unmarkPending(pack: Package): Free[F, Unit] = Free.inject[Ops, F](UnmarkPending(pack))

    def unmarkPendingMany(packages: List[Package]): Free[F, Unit] =
      Free.inject[Ops, F](UnmarkPendingMany(packages))

    def markError(pack: Package): Free[F, Unit] = Free.inject[Ops, F](MarkError(pack))

    def markErrorMany(packages: List[Package]): Free[F, Unit] =
      Free.inject[Ops, F](MarkErrorMany(packages))

    def clearInvalid(pack: Package): Free[F, Unit] = Free.inject[Ops, F](ClearInvalid(pack))

    def clearInvalidMany(packages: List[Package]): Free[F, Unit] =
      Free.inject[Ops, F](ClearInvalidMany(packages))

    def isPending(pack: Package): Free[F, Boolean] = Free.inject[Ops, F](IsPending(pack))

    def listPending(limit: Int): Free[F, List[Package]] = Free.inject[Ops, F](ListPending(limit))

  }

  object Service {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Service[F] = new Service[F]
  }

}