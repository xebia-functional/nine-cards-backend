package com.fortysevendeg.ninecards.googleplay.service.free.algebra

import cats.data.Xor
import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.domain.apigoogle._

package apigoogle {

  sealed trait Ops[A]

  case class GetDetails( packageName: Package, authParams: GoogleAuthParams)
      extends Ops[Failure Xor FullCard]

  class Service[F[_]](implicit inj: Inject[Ops, F]) {

    def getDetails( packageName: Package, auth: GoogleAuthParams): Free[F, Failure Xor FullCard] =
      Free.inject[Ops, F](GetDetails(packageName, auth))

  }

  object Service {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Service[F] = new Service[F]
  }

}
