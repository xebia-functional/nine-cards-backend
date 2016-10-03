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
      extends Ops[InfoError Xor FullCard]

  case class GetCardList(auth: GoogleAuthParams, packageList: PackageList)
      extends Ops[FullCardList]

  case class RecommendationsByCategory(auth: GoogleAuthParams, request: RecommendByCategoryRequest )
      extends Ops[InfoError Xor FullCardList]

  case class RecommendationsByAppList(auth: GoogleAuthParams, request: RecommendByAppsRequest )
      extends Ops[FullCardList]

  class Service[F[_]](implicit I: Inject[Ops, F]) {

    def resolve(auth: GoogleAuthParams, pkg: Package): Free[F, Option[Item]] =
      Free.inject[Ops, F](Resolve(auth, pkg))
    def resolveMany(auth: GoogleAuthParams, packageList: PackageList): Free[F, PackageDetails] =
      Free.inject[Ops, F](ResolveMany(auth, packageList))

    def getCard(auth: GoogleAuthParams, pkg: Package): Free[F,Xor[InfoError, FullCard]] =
      Free.inject[Ops, F](GetCard(auth, pkg))

    def getCardList(auth: GoogleAuthParams, packageList: PackageList): Free[F, FullCardList] =
      Free.inject[Ops, F](GetCardList(auth, packageList))

    def recommendationsByCategory (
      auth: GoogleAuthParams, request: RecommendByCategoryRequest
    ):  Free[F, Xor[InfoError, FullCardList]] =
      Free.inject[Ops, F](RecommendationsByCategory(auth, request))

    def recommendationsByAppList(auth: GoogleAuthParams, request: RecommendByAppsRequest) :
        Free[F, FullCardList] =
      Free.inject[Ops, F](RecommendationsByAppList(auth, request))
  }

  object Service {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Service[F] = new Service[F]
  }

  type FreeOps[A] = Free[Ops, A]

}
