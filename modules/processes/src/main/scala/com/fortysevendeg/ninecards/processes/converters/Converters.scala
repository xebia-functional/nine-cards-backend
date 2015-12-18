package com.fortysevendeg.ninecards.processes.converters

import com.fortysevendeg.ninecards.processes.InstallationRequest
import com.fortysevendeg.ninecards.processes.domain._
import com.fortysevendeg.ninecards.services.free.domain.{
GooglePlayApp => GooglePlayAppServices,
User => UserAppServices,
AuthData => AuthDataSevices,
TwitterAuthData => TwitterAuthDataApp,
FacebookAuthData => FacebookAuthDataApp,
AnonymousAuthData => AnonymousAuthDataApp,
GoogleAuthData => GoogleAuthDataApp,
GoogleAuthDataDeviceInfo => GoogleAuthDataDeviceInfoApp,
GoogleOAuth2Data => GoogleOAuth2DataApp,
Installation => InstallationServices
}

object Converters {

  def toGooglePlayApp(app: GooglePlayAppServices): GooglePlayApp =
    GooglePlayApp(
      packageName = app.packageName,
      appType = app.appType,
      appCategory = app.appCategory,
      numDownloads = app.numDownloads,
      starRating = app.starRating,
      ratingCount = app.ratingCount,
      commentCount = app.commentCount)

  def toUserApp(app: UserAppServices): User =
    User(
      id = app.id,
      username = app.username,
      password = app.password,
      email = app.email,
      sessionToken = app.sessionToken,
      authData = app.authData map toAuthDataApp)

  def toAuthDataApp(app: AuthDataSevices): AuthData =
    AuthData(
      twitter = app.twitter map toTwitterAuthDataApp,
      facebook = app.facebook map toFacebookAuthDataApp,
      anonymous = app.anonymous map toAnonymousAuthDataApp,
      google = app.google map toGoogleAuthDataApp,
      googleOAuth2 = app.googleOAuth2 map toGoogleOAuth2DataApp)

  def toTwitterAuthDataApp(app: TwitterAuthDataApp): TwitterAuthData =
    TwitterAuthData(
      id = app.id,
      screenName = app.screenName,
      consumerKey = app.consumerKey,
      consumerSecret = app.consumerSecret,
      authToken = app.authToken,
      authTokenSecret = app.authTokenSecret)

  def toFacebookAuthDataApp(app: FacebookAuthDataApp): FacebookAuthData =
    FacebookAuthData(
      id = app.id,
      accessToken = app.accessToken,
      expirationDate = app.expirationDate)

  def toAnonymousAuthDataApp(app: AnonymousAuthDataApp): AnonymousAuthData =
    AnonymousAuthData(
      id = app.id)

  def toGoogleAuthDataApp(app: GoogleAuthDataApp): GoogleAuthData =
    GoogleAuthData(
      email = app.email,
      devices = app.devices map toGoogleAuthDataDeviceInfoApp)

  def toGoogleAuthDataDeviceInfoApp(app: GoogleAuthDataDeviceInfoApp): GoogleAuthDataDeviceInfo =
    GoogleAuthDataDeviceInfo(
      name = app.name,
      deviceId = app.deviceId,
      secretToken = app.secretToken,
      permissions = app.permissions)

  def toGoogleOAuth2DataApp(app: GoogleOAuth2DataApp): GoogleOAuth2Data =
    GoogleOAuth2Data(
      id = app.id,
      accessToken = app.accessToken,
      expirationDate = app.expirationDate)


  def toInstallationRequest(app: InstallationRequest): InstallationServices =
    InstallationServices(
      id = app.id,
      deviceType = app.deviceType,
      deviceToken = app.deviceToken,
      userId = app.userId,
      channels = app.channels
    )
  def fromInstallationProcesses(app: InstallationServices): Installation =
    Installation(
      id = app.id,
      deviceType = app.deviceType,
      deviceToken = app.deviceToken,
      userId = app.userId,
      channels = app.channels
    )
}


