package cards.nine.processes.account

import cards.nine.domain.account._

package messages {

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

  case class UpdateInstallationRequest(
    userId: Long,
    androidId: AndroidId,
    deviceToken: Option[DeviceToken]
  )

  case class UpdateInstallationResponse(
    androidId: AndroidId,
    deviceToken: Option[DeviceToken]
  )

}
