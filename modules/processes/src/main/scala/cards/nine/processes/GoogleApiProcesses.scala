package cards.nine.processes

import cards.nine.domain.account.{ Email, GoogleIdToken }
import cards.nine.services.free.algebra.GoogleApi
import cats.free.Free

class GoogleApiProcesses[F[_]](implicit googleAPIServices: GoogleApi.Services[F]) {

  def checkGoogleTokenId(email: Email, tokenId: GoogleIdToken): Free[F, Boolean] =
    googleAPIServices.getTokenInfo(tokenId) map { xor ⇒
      xor.exists(tokenInfo ⇒ tokenInfo.email_verified == "true" && tokenInfo.email == email.value)
    }
}

object GoogleApiProcesses {

  implicit def googleApiProcesses[F[_]](implicit googleAPIServices: GoogleApi.Services[F]) =
    new GoogleApiProcesses

}
