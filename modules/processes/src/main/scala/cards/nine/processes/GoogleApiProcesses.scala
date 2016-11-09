package cards.nine.processes

import cards.nine.commons.NineCardsService.NineCardsService
import cards.nine.domain.account.{ Email, GoogleIdToken }
import cards.nine.services.free.algebra.GoogleApi

class GoogleApiProcesses[F[_]](implicit googleAPIServices: GoogleApi.Services[F]) {

  def checkGoogleTokenId(email: Email, tokenId: GoogleIdToken): NineCardsService[F, Boolean] =
    googleAPIServices.getTokenInfo(tokenId) map { tokenInfo ⇒
      tokenInfo.email_verified == "true" && tokenInfo.email == email.value
    }
}

object GoogleApiProcesses {

  implicit def googleApiProcesses[F[_]](implicit googleAPIServices: GoogleApi.Services[F]) =
    new GoogleApiProcesses

}