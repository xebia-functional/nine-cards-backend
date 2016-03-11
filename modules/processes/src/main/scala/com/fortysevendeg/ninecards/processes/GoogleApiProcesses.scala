package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.services.free.algebra.GoogleApiServices.GoogleApiServices

class GoogleApiProcesses[F[_]](
  implicit googleAPIServices: GoogleApiServices[F]) {

  def checkGoogleTokenId(email: String, tokenId: String): Free[F, Boolean] =
    googleAPIServices.getTokenInfo(tokenId) map { xor =>
      xor.exists(tokenInfo => tokenInfo.email_verified == "true" && tokenInfo.email == email)
    }
}

object GoogleApiProcesses {

  implicit def googleApiProcesses[F[_]](
    implicit googleAPIServices: GoogleApiServices[F]) = new GoogleApiProcesses

}
