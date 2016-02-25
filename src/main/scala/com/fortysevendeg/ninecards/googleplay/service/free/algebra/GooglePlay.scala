package com.fortysevendeg.ninecards.googleplay.service.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._

object GooglePlay {

  sealed trait GooglePlayOps[A]
  case class RequestPackage(auth: GoogleAuthParams, pkg: Package) extends GooglePlayOps[Option[Item]] // todo remember this will probably not stay as option
  case class BulkRequestPackage(auth: GoogleAuthParams, packageListRequest: PackageListRequest) extends GooglePlayOps[PackageDetails]

  class GooglePlayService[F[_]](implicit I: Inject[GooglePlayOps, F]) {
    def requestPackage(auth: GoogleAuthParams, pkg: Package): Free[F, Option[Item]] = Free.inject[GooglePlayOps, F](RequestPackage(auth, pkg))
    def bulkRequestPackage(auth: GoogleAuthParams, packageListRequest: PackageListRequest): Free[F, PackageDetails] = Free.inject[GooglePlayOps, F](BulkRequestPackage(auth, packageListRequest))
  }

  object GooglePlayService {
    implicit def googlePlay[F[_]](implicit I: Inject[GooglePlayOps, F]): GooglePlayService[F] = new GooglePlayService[F]
  }

}
