package com.fortysevendeg.ninecards.googleplay.service.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.googleplay.domain.{Package, FullCard}
import org.joda.time.DateTime

object cache {

  sealed trait Ops[A]

  case class GetValid(pack: Package) extends Ops[Option[FullCard]]

  case class PutResolved(card: FullCard) extends Ops[Unit]

  case class MarkPending(`package`: Package) extends Ops[Unit]

  case class MarkError(`package`: Package, date: DateTime) extends Ops[Unit]

  case class ClearInvalid(`package`: Package) extends Ops[Unit]

  class Service[F[_]](implicit I: Inject[Ops, F]) {

    def getValid( pack: Package) : Free[F, Option[FullCard]] =
      Free.inject[Ops, F](GetValid(pack))

    def putResolved(card: FullCard) : Free[F, Unit] =
      Free.inject[Ops, F](PutResolved(card) )

    def markPending(pack: Package): Free[F, Unit] =
      Free.inject[Ops, F](MarkPending(pack) )

    def markError(pack: Package, date: DateTime): Free[F, Unit] =
      Free.inject[Ops, F](MarkError(pack, date) )

    def clearInvalid(pack: Package) : Free[F, Unit] =
      Free.inject[Ops, F](ClearInvalid(pack) )
  }

  object Service {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Service[F] = new Service[F]
  }

}