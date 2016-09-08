package com.fortysevendeg.ninecards.googleplay.service.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.googleplay.domain.{Package, FullCard}

object cache {

  sealed trait Ops[A]

  case class GetValid(pack: Package) extends Ops[Option[FullCard]]

  class Service[F[_]](implicit I: Inject[Ops, F]) {

    def getValid( pack: Package) : Free[F, Option[FullCard]] =
      Free.inject[Ops, F](GetValid(pack))
  }

  object Service {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Service[F] = new Service[F]
  }

}