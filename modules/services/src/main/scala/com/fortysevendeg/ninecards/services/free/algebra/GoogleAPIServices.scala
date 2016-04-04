package com.fortysevendeg.ninecards.services.free.algebra

import cats.data.Xor
import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.domain.{TokenInfo, WrongTokenInfo}

object GoogleApiServices {

  sealed trait GoogleApiOps[A]

  case class GetTokenInfo(tokenId: String) extends GoogleApiOps[WrongTokenInfo Xor TokenInfo]

  class GoogleApiServices[F[_]](implicit I: Inject[GoogleApiOps, F]) {

    def getTokenInfo(
      tokenId: String
    ): Free[F, WrongTokenInfo Xor TokenInfo] = Free.inject[GoogleApiOps, F](GetTokenInfo(tokenId))

  }

  object GoogleApiServices {

    implicit def googleAPIServices[F[_]](
      implicit
      I: Inject[GoogleApiOps, F]
    ): GoogleApiServices[F] = new GoogleApiServices

  }

}
