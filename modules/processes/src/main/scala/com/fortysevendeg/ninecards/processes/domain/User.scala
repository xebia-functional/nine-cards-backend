package com.fortysevendeg.ninecards.processes.domain

case class AuthData(
  twitter: Option[TwitterAuthData] = None,
  facebook: Option[FacebookAuthData] = None,
  anonymous: Option[AnonymousAuthData] = None,
  google: Option[GoogleAuthData] = None,
  googleOAuth2: Option[GoogleOAuth2Data] = None)

case class GoogleAuthData(
  email: String,
  devices: List[GoogleAuthDataDeviceInfo])

case class GoogleAuthDataDeviceInfo(
  name: String,
  deviceId: String,
  secretToken: String,
  permissions: List[String] = Nil)

case class GoogleOAuth2Data(
  id: String,
  accessToken: String,
  expirationDate: Long)

case class FacebookAuthData(
  id: String,
  accessToken: String,
  expirationDate: Long)

case class TwitterAuthData(
  id: String,
  screenName: String,
  consumerKey: String,
  consumerSecret: String,
  authToken: String,
  authTokenSecret: String)

case class AnonymousAuthData(id: String)

case class User(
  _id: Option[String] = None,
  username: Option[String] = None,
  password: Option[String] = None,
  email: Option[String] = None,
  sessionToken: Option[String] = None,
  authData: Option[AuthData] = None)

case class Installation(
  _id: Option[String] = None,
  deviceType: String,
  deviceToken: Option[String] = None,
  userId: Option[String] = None,
  channels:List[String] = Nil)