package com.fortysevendeg.ninecards.processes.messages

object DevicesMessages {

  case class UpdateDeviceRequest(userId: Long, androidId: String, deviceToken: Option[String])

  case class UpdateDeviceResponse(androidId: String, deviceToken: Option[String])
}
