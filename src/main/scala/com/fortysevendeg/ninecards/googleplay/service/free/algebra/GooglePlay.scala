package com.fortysevendeg.ninecards.googleplay.service.free.algebra

import cats.free.{Free, Inject}
import scala.language.higherKinds // todo put in sbt
import com.fortysevendeg.ninecards.googleplay.domain.Domain._

object GooglePlay {

  sealed trait GooglePlayOps[A]
  case class RequestPackage(pkg: Package) extends GooglePlayOps[Option[Item]] // todo remember this will probably not stay as option
  case class BulkRequestPackage(packageListRequest: PackageListRequest) extends GooglePlayOps[PackageDetails]

  class GooglePlayService[F[_]](implicit I: Inject[GooglePlayOps, F]) {
    def requestPackage(pkg: Package): Free[F, Option[Item]] = Free.inject[GooglePlayOps, F](RequestPackage(pkg))
    def bulkRequestPackage(packageListRequest: PackageListRequest): Free[F, PackageDetails] = Free.inject[GooglePlayOps, F](BulkRequestPackage(packageListRequest))
  }

  object GooglePlayService {
    implicit def googlePlay[F[_]](implicit I: Inject[GooglePlayOps, F]): GooglePlayService[F] = new GooglePlayService[F]
  }

}
