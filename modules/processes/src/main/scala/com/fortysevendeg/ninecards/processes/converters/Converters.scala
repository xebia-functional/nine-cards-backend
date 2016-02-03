package com.fortysevendeg.ninecards.processes.converters

import com.fortysevendeg.ninecards.processes.domain._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages.LoginResponse
import com.fortysevendeg.ninecards.services.free.domain.{
GooglePlayApp => GooglePlayAppServices,
Installation => InstallationServices,
User => UserAppServices}

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

  def toLoginResponse(info: (UserAppServices, InstallationServices)): LoginResponse = {
    val (user, _) = info
    LoginResponse(
      sessionToken = user.sessionToken)
  }

  def toUpdateInstallationResponse(installation: InstallationServices): UpdateInstallationResponse =
    UpdateInstallationResponse(
      androidId = installation.androidId,
      deviceToken = installation.deviceToken)

}
