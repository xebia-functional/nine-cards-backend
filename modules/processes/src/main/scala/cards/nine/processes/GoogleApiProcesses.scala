package cards.nine.processes

import cats.free.Free
import cards.nine.services.free.algebra.GoogleApi

class GoogleApiProcesses[F[_]](implicit googleAPIServices: GoogleApi.Services[F]) {

  def checkGoogleTokenId(email: String, tokenId: String): Free[F, Boolean] =
    googleAPIServices.getTokenInfo(tokenId) map { xor ⇒
      xor.exists(tokenInfo ⇒ tokenInfo.email_verified == "true" && tokenInfo.email == email)
    }
}

object GoogleApiProcesses {

  implicit def googleApiProcesses[F[_]](implicit googleAPIServices: GoogleApi.Services[F]) =
    new GoogleApiProcesses

}
