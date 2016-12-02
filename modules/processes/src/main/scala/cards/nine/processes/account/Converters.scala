package cards.nine.processes.account

import cards.nine.services.free.domain.{ Installation, User }

private[account] object Converters {

  import messages._

  def toLoginResponse(info: (User, Installation)): LoginResponse = {
    val (user, _) = info
    LoginResponse(
      apiKey       = user.apiKey,
      sessionToken = user.sessionToken
    )
  }

  def toUpdateInstallationResponse(installation: Installation): UpdateInstallationResponse =
    UpdateInstallationResponse(
      androidId   = installation.androidId,
      deviceToken = installation.deviceToken
    )

}
