package com.fortysevendeg.ninecards.api.converters

import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._

import scala.language.implicitConversions

object Converters {

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
