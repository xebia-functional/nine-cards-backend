package cards.nine.processes.messages

object InstallationsMessages {

  case class UpdateInstallationRequest(userId: Long, androidId: String, deviceToken: Option[String])

  case class UpdateInstallationResponse(androidId: String, deviceToken: Option[String])
}
