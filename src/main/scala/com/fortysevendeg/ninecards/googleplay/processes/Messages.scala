package com.fortysevendeg.ninecards.googleplay.processes

import cats.data.Xor
import com.fortysevendeg.ninecards.googleplay.domain.{Package, FullCard, GoogleAuthParams}

package object getcard {
  sealed trait FailedResponse
  case class WrongAuthParams( authParams: GoogleAuthParams) extends FailedResponse
  case class PendingResolution( packageName: Package) extends FailedResponse
  case class UnknownPackage( packageName: Package) extends FailedResponse
  type Response = FailedResponse Xor FullCard
}

object ResolvePending {

  case class Response(solved: List[Package], unknown: List[Package], pending: List[Package])

  sealed trait PackageStatus
  case object Resolved extends PackageStatus
  case object Pending extends PackageStatus
  case object Unknown extends PackageStatus
}

