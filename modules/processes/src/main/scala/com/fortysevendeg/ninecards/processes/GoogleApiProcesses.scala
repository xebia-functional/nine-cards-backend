package com.fortysevendeg.ninecards.processes

import cats.data.Xor
import cats.free.Free
import com.fortysevendeg.ninecards.services.free.algebra.GoogleApiServices.GoogleApiServices

class GoogleApiProcesses[F[_]](
  implicit googleAPIServices: GoogleApiServices[F]) {

  def checkGoogleTokenId(email: String, tokenId: String): Free[F, Boolean] =
    googleAPIServices.getTokenInfo(tokenId) map {
      case Xor.Left(_) =>
        false
      case Xor.Right(tokenInfo) =>
        if (tokenInfo.email_verified == "true" && tokenInfo.email == email)
          true
        else
          false
    }
}

object GoogleApiProcesses {

  implicit def googleApiProcesses[F[_]](
    implicit googleAPIServices: GoogleApiServices[F]) = new GoogleApiProcesses

}
