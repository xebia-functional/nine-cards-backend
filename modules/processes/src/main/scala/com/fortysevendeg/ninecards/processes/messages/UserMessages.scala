package com.fortysevendeg.ninecards.processes.messages

object UserMessages {

  case class LoginRequest(
    email: String,
    androidId: String,
    oauthToken: String)

  case class LoginResponse(
    sessionToken: String)

}
