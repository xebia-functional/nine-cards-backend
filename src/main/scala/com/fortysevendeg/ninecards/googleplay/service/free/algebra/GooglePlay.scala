package com.fortysevendeg.ninecards.googleplay.service.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.googleplay.domain._

object GooglePlay {

  sealed trait Ops[A]
  // todo remember this will probably not stay as option
  case class Resolve(auth: GoogleAuthParams, pkg: Package) extends Ops[Option[Item]]
  case class ResolveMany(auth: GoogleAuthParams, packageList: PackageList) extends Ops[PackageDetails]

  class Service[F[_]](implicit I: Inject[Ops, F]) {

    def resolve(auth: GoogleAuthParams, pkg: Package): Free[F, Option[Item]] =
      Free.inject[Ops, F](Resolve(auth, pkg))
    def resolveMany(auth: GoogleAuthParams, packageList: PackageList): Free[F, PackageDetails] =
      Free.inject[Ops, F](ResolveMany(auth, packageList))
  }

  object Service {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Service[F] = new Service[F]
  }

}
