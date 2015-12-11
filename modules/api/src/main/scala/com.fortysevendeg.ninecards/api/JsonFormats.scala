package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.processes.domain.GooglePlayApp
import com.fortysevendeg.ninecards.processes.messages.{AuthDataRequest, GoogleAuthDataDeviceInfoRequest, GoogleAuthDataRequest, AddUserRequest}
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

  implicit val googlePlayApp = jsonFormat7(GooglePlayApp)

  implicit val googleAuthDataDeviceInfoRequest = jsonFormat4(GoogleAuthDataDeviceInfoRequest)

  implicit val googleAuthDataRequest = jsonFormat2(GoogleAuthDataRequest)

  implicit val authDataRequest = jsonFormat1(AuthDataRequest)

  implicit val addUserRequest = jsonFormat1(AddUserRequest)


}
