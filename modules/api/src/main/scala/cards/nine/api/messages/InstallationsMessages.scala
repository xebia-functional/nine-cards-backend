package cards.nine.api.messages

import cards.nine.domain.account.{ AndroidId, DeviceToken }

object InstallationsMessages {

  case class ApiUpdateInstallationRequest(deviceToken: Option[DeviceToken])

  case class ApiUpdateInstallationResponse(androidId: AndroidId, deviceToken: Option[DeviceToken])
}
