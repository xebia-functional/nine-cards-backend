package cards.nine.api.accounts

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.accounts.messages._
import cards.nine.domain.account._
import cards.nine.processes.account.messages._

private[accounts] object Converters {

  def toLoginRequest(request: ApiLoginRequest, sessionToken: SessionToken): LoginRequest =
    LoginRequest(
      email        = request.email,
      androidId    = request.androidId,
      sessionToken = sessionToken,
      tokenId      = request.tokenId
    )

  def toApiLoginResponse(response: LoginResponse): ApiLoginResponse =
    ApiLoginResponse(
      apiKey       = response.apiKey,
      sessionToken = response.sessionToken
    )

  def toUpdateInstallationRequest(
    request: ApiUpdateInstallationRequest,
    userContext: UserContext
  ): UpdateInstallationRequest =
    UpdateInstallationRequest(
      userId      = userContext.userId.value,
      androidId   = userContext.androidId,
      deviceToken = request.deviceToken
    )

  def toApiUpdateInstallationResponse(
    response: UpdateInstallationResponse
  ): ApiUpdateInstallationResponse =
    ApiUpdateInstallationResponse(
      androidId   = response.androidId,
      deviceToken = response.deviceToken
    )

}
