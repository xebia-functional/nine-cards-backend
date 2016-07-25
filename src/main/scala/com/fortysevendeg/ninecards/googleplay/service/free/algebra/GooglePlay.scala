package com.fortysevendeg.ninecards.googleplay.service.free.algebra

import cats.data.Xor
import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.googleplay.domain._

object GooglePlay {

  sealed trait Ops[A]
  // todo remember this will probably not stay as option
  case class Resolve(auth: GoogleAuthParams, pkg: Package) extends Ops[Option[Item]]
  case class ResolveMany(auth: GoogleAuthParams, packageList: PackageList) extends Ops[PackageDetails]

  case class GetCard(auth: GoogleAuthParams, pkg: Package)
      extends Ops[InfoError Xor AppCard]

  case class GetCardList(auth: GoogleAuthParams, packageList: PackageList)
      extends Ops[AppCardList]

  class Service[F[_]](implicit I: Inject[Ops, F]) {

    def resolve(auth: GoogleAuthParams, pkg: Package): Free[F, Option[Item]] =
      Free.inject[Ops, F](Resolve(auth, pkg))
    def resolveMany(auth: GoogleAuthParams, packageList: PackageList): Free[F, PackageDetails] =
      Free.inject[Ops, F](ResolveMany(auth, packageList))

    def getCard(auth: GoogleAuthParams, pkg: Package): Free[F,Xor[InfoError, AppCard]] =
      Free.inject[Ops, F](GetCard(auth, pkg))

    def getCardList(auth: GoogleAuthParams, packageList: PackageList): Free[F, AppCardList] =
      Free.inject[Ops, F](GetCardList(auth, packageList))

  }

  object Service {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Service[F] = new Service[F]
  }

}
