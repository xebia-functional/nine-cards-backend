package com.fortysevendeg.ninecards.processes.messages

case class AddUserRequest(
  authData: AuthDataRequest)

case class AuthDataRequest(
  google: GoogleAuthDataRequest)

case class GoogleAuthDataRequest(
  email: String,
  devices: List[GoogleAuthDataDeviceInfoRequest])

case class GoogleAuthDataDeviceInfoRequest(
  name: String,
  deviceId: String,
  secretToken: String,
  permissions: List[String])

case class UpdateGoogleAuthDataDeviceInfoRequest(
  name: String,
  secretToken: String,
  permissions: List[String])