package cards.nine.api.messages

import cards.nine.domain.account._

object UserMessages {

  case class ApiLoginRequest(
    email: Email,
    androidId: AndroidId,
    tokenId: GoogleIdToken
  )

  case class ApiLoginResponse(
    apiKey: ApiKey,
    sessionToken: SessionToken
  )
}
