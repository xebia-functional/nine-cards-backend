package com.fortysevendeg.ninecards.googleplay.service.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.googleplay.domain._

object webscrapper {

  sealed trait Ops[A]

  case class ExistsApp( pack: Package) extends Ops[Boolean]

  class Service[ F[_]](implicit i: Inject[Ops, F]) {

    def existsApp( pack: Package): Free[F, Boolean] =
      Free.inject[Ops, F](ExistsApp(pack))

  }

  object Service {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Service[F] = new Service[F]
  }

}
