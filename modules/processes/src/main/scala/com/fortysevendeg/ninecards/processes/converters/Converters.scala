package com.fortysevendeg.ninecards.processes.converters

import com.fortysevendeg.ninecards.processes.domain._
import com.fortysevendeg.ninecards.processes.messages.{GoogleAuthDataDeviceInfoRequest, GoogleAuthDataRequest, AuthDataRequest, AddUserRequest}
import com.fortysevendeg.ninecards.services.free.domain.{
GooglePlayApp => GooglePlayAppServices,
User => UserAppServices,
AuthData => AuthDataSevices,
TwitterAuthData => TwitterAuthDataApp,
FacebookAuthData => FacebookAuthDataApp,
AnonymousAuthData => AnonymousAuthDataApp,
GoogleAuthData => GoogleAuthDataApp,
GoogleAuthDataDeviceInfo => GoogleAuthDataDeviceInfoApp,
GoogleOAuth2Data => GoogleOAuth2DataApp
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

  def fromUserApp(app: User): UserAppServices =
    UserAppServices(
      id = app.id,
      username = app.username,
      password = app.password,
      email = app.email,
      sessionToken = app.sessionToken,
      authData = app.authData map fromAuthDataApp)

  def toAuthDataApp(app: AuthDataSevices): AuthData =
    AuthData(
      twitter = app.twitter map toTwitterAuthDataApp,
      facebook = app.facebook map toFacebookAuthDataApp,
      anonymous = app.anonymous map toAnonymousAuthDataApp,
      google = app.google map toGoogleAuthDataApp,
      googleOAuth2 = app.googleOAuth2 map toGoogleOAuth2DataApp)

  def fromAuthDataApp(app: AuthData): AuthDataSevices =
    AuthDataSevices(
      twitter = app.twitter map fromTwitterAuthDataApp,
      facebook = app.facebook map fromFacebookAuthDataApp,
      anonymous = app.anonymous map fromAnonymousAuthDataApp,
      google = app.google map fromGoogleAuthDataApp,
      googleOAuth2 = app.googleOAuth2 map fromGoogleOAuth2DataApp)

  def toTwitterAuthDataApp(app: TwitterAuthDataApp): TwitterAuthData =
    TwitterAuthData(
      id = app.id,
      screenName = app.screenName,
      consumerKey = app.consumerKey,
      consumerSecret = app.consumerSecret,
      authToken = app.authToken,
      authTokenSecret = app.authTokenSecret)

  def fromTwitterAuthDataApp(app: TwitterAuthData): TwitterAuthDataApp =
    TwitterAuthDataApp(
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

  def fromFacebookAuthDataApp(app: FacebookAuthData): FacebookAuthDataApp =
    FacebookAuthDataApp(
      id = app.id,
      accessToken = app.accessToken,
      expirationDate = app.expirationDate)

  def toAnonymousAuthDataApp(app: AnonymousAuthDataApp): AnonymousAuthData =
    AnonymousAuthData(
      id = app.id)

  def fromAnonymousAuthDataApp(app: AnonymousAuthData): AnonymousAuthDataApp =
    AnonymousAuthDataApp(
      id = app.id)

  def toGoogleAuthDataApp(app: GoogleAuthDataApp): GoogleAuthData =
    GoogleAuthData(
      email = app.email,
      devices = app.devices map toGoogleAuthDataDeviceInfoApp)

  def fromGoogleAuthDataApp(app: GoogleAuthData): GoogleAuthDataApp =
    GoogleAuthDataApp(
      email = app.email,
      devices = app.devices map fromGoogleAuthDataDeviceInfoApp)

  def toGoogleAuthDataDeviceInfoApp(app: GoogleAuthDataDeviceInfoApp): GoogleAuthDataDeviceInfo =
    GoogleAuthDataDeviceInfo(
      name = app.name,
      deviceId = app.deviceId,
      secretToken = app.secretToken,
      permissions = app.permissions)

  def fromGoogleAuthDataDeviceInfoApp(app: GoogleAuthDataDeviceInfo): GoogleAuthDataDeviceInfoApp =
    GoogleAuthDataDeviceInfoApp(
      name = app.name,
      deviceId = app.deviceId,
      secretToken = app.secretToken,
      permissions = app.permissions)

  def toGoogleOAuth2DataApp(app: GoogleOAuth2DataApp): GoogleOAuth2Data =
    GoogleOAuth2Data(
      id = app.id,
      accessToken = app.accessToken,
      expirationDate = app.expirationDate)

  def fromGoogleOAuth2DataApp(app: GoogleOAuth2Data): GoogleOAuth2DataApp =
    GoogleOAuth2DataApp(
      id = app.id,
      accessToken = app.accessToken,
      expirationDate = app.expirationDate)

  def toUserRequestApp(app: AddUserRequest): UserAppServices =
    UserAppServices(
      authData = Option(toAuthDataRequestApp(app.authData)))

  def toAuthDataRequestApp(app: AuthDataRequest): AuthDataSevices =
    AuthDataSevices(
      google = Option(toGoogleAuthDataRequestApp(app.google)))

  def toGoogleAuthDataRequestApp(app: GoogleAuthDataRequest): GoogleAuthDataApp =
    GoogleAuthDataApp(
      email = app.email,
      devices = app.devices map toGoogleAuthDataDeviceInfoRequestApp)

  def toGoogleAuthDataDeviceInfoRequestApp(app: GoogleAuthDataDeviceInfoRequest): GoogleAuthDataDeviceInfoApp =
    GoogleAuthDataDeviceInfoApp(
      name = app.name,
      deviceId = app.deviceId,
      secretToken = app.secretToken,
      permissions = app.permissions)

  def toGoogleAuthDataRequestProcess(app: GoogleAuthDataRequest): GoogleAuthDataApp =
    GoogleAuthDataApp(
      email = app.email,
      devices = app.devices map toGoogleAuthDataDeviceInfoRequestProcess)

  def toGoogleAuthDataDeviceInfoRequestProcess(app: GoogleAuthDataDeviceInfoRequest): GoogleAuthDataDeviceInfoApp =
    GoogleAuthDataDeviceInfoApp(
      name = app.name,
      deviceId = app.deviceId,
      secretToken = app.secretToken,
      permissions = app.permissions)

}
