package com.fortysevendeg.ninecards.api.converters

import com.fortysevendeg.ninecards.api.messages.DevicesMessages._
import com.fortysevendeg.ninecards.processes.messages.DevicesMessages._

import scala.language.implicitConversions

object Converters {

  implicit def toUpdateDeviceRequest(
    request: ApiUpdateDeviceRequest)(implicit userId: Long, androidId: String): UpdateDeviceRequest =
    UpdateDeviceRequest(
      userId = userId,
      androidId = androidId,
      deviceToken = request.deviceToken)

  implicit def toApiUpdateDeviceResponse(
    response: UpdateDeviceResponse): ApiUpdateDeviceResponse =
    ApiUpdateDeviceResponse(
      androidId = response.androidId,
      deviceToken = response.deviceToken)

}
