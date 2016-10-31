package cards.nine.processes.messages

import cards.nine.domain.account.{ AndroidId, DeviceToken }

object InstallationsMessages {

  case class UpdateInstallationRequest(userId: Long, androidId: AndroidId, deviceToken: Option[DeviceToken])

  case class UpdateInstallationResponse(androidId: AndroidId, deviceToken: Option[DeviceToken])
}
