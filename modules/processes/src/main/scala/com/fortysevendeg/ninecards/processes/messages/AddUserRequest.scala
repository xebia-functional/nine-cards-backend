package com.fortysevendeg.ninecards.processes.messages

case class AddUserRequest(
  email: String,
  androidId: String,
  oauthToken: String)

case class UserResponse(
  sessionToken: String)
