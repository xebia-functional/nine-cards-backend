package com.fortysevendeg.ninecards.api.messages

object UserMessages {

  case class ApiLoginRequest(
    email: String,
    androidId: String,
    oauthToken: String)

  case class ApiLoginResponse(
    sessionToken: String)
}
