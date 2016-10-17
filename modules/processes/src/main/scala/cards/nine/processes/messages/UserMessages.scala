package cards.nine.processes.messages

import cards.nine.domain.account._

object UserMessages {

  case class LoginRequest(
    email: Email,
    androidId: AndroidId,
    sessionToken: SessionToken,
    tokenId: GoogleIdToken
  )

  case class LoginResponse(
    apiKey: ApiKey,
    sessionToken: SessionToken
  )

}
