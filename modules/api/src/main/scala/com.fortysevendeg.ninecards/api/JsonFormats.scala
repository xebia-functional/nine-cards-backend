package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.api.messages.DevicesMessages._
import com.fortysevendeg.ninecards.processes.messages._
import com.fortysevendeg.ninecards.processes.domain._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

  implicit val googlePlayAppFormat = jsonFormat7(GooglePlayApp)

  implicit val userFormat = jsonFormat3(User)

  implicit val addUserRequest = jsonFormat1(AddUserRequest)

  implicit val updateDeviceRequestFormat = jsonFormat1(ApiUpdateDeviceRequest)

  implicit val updateDeviceResponseFormat = jsonFormat2(ApiUpdateDeviceResponse)
}
