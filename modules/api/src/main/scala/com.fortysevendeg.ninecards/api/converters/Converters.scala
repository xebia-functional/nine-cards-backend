package com.fortysevendeg.ninecards.api.converters

import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.UserMessages._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages._

import scala.language.implicitConversions

object Converters {

  implicit def toLoginRequest(
    request: ApiLoginRequest): LoginRequest =
    LoginRequest(
      email = request.email,
      androidId = request.androidId,
      oauthToken = request.oauthToken)

  implicit def toApiLoginResponse(
    response: LoginResponse): ApiLoginResponse =
    ApiLoginResponse(
      sessionToken = response.sessionToken)

  implicit def toUpdateInstallationRequest(
    request: ApiUpdateInstallationRequest)(
    implicit userId: Long, androidId: String): UpdateInstallationRequest =
    UpdateInstallationRequest(
      userId = userId,
      androidId = androidId,
      deviceToken = request.deviceToken)

  implicit def toApiUpdateInstallationResponse(
    response: UpdateInstallationResponse): ApiUpdateInstallationResponse =
    ApiUpdateInstallationResponse(
      androidId = response.androidId,
      deviceToken = response.deviceToken)

}
