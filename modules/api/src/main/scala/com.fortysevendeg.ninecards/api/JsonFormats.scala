package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.UserMessages._
import com.fortysevendeg.ninecards.processes.domain._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

  implicit val googlePlayAppFormat = jsonFormat7(GooglePlayApp)

  implicit val apiLoginRequestFormat = jsonFormat3(ApiLoginRequest)

  implicit val apiLoginResponseFormat = jsonFormat1(ApiLoginResponse)

  implicit val updateInstallationRequestFormat = jsonFormat1(ApiUpdateInstallationRequest)

  implicit val updateInstallationResponseFormat = jsonFormat2(ApiUpdateInstallationResponse)
}
