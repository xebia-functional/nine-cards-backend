package com.fortysevendeg.ninecards.api.messages

object DevicesMessages {

  case class ApiUpdateDeviceRequest(deviceToken: Option[String])

  case class ApiUpdateDeviceResponse(androidId: String, deviceToken: Option[String])
}
