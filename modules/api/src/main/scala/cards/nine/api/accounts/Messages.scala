package cards.nine.api.accounts

import cards.nine.domain.account._

object messages {

  case class ApiLoginRequest(
    email: Email,
    androidId: AndroidId,
    tokenId: GoogleIdToken
  )

  case class ApiLoginResponse(
    apiKey: ApiKey,
    sessionToken: SessionToken
  )

  case class ApiUpdateInstallationRequest(deviceToken: Option[DeviceToken])

  case class ApiUpdateInstallationResponse(androidId: AndroidId, deviceToken: Option[DeviceToken])

}
