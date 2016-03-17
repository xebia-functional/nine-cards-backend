package com.fortysevendeg.ninecards.api.converters

import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain._
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.UserMessages._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages._

object Converters {

  implicit def toLoginRequest(
    request: ApiLoginRequest): LoginRequest =
    LoginRequest(
      email = request.email,
      androidId = request.androidId,
      tokenId = request.tokenId)

  implicit def toApiLoginResponse(
    response: LoginResponse): ApiLoginResponse =
    ApiLoginResponse(
      apiKey = response.apiKey,
      sessionToken = response.sessionToken)

  implicit def toUpdateInstallationRequest(
    request: ApiUpdateInstallationRequest)(
    implicit userId: UserId, androidId: AndroidId): UpdateInstallationRequest =
    UpdateInstallationRequest(
      userId = userId.value,
      androidId = androidId.value,
      deviceToken = request.deviceToken)

  implicit def toApiUpdateInstallationResponse(
    response: UpdateInstallationResponse): ApiUpdateInstallationResponse =
    ApiUpdateInstallationResponse(
      androidId = response.androidId,
      deviceToken = response.deviceToken)

}
