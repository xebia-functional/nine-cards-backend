package com.fortysevendeg.ninecards.processes.converters

import com.fortysevendeg.ninecards.processes.domain._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.services.free.domain.{
Installation => InstallationServices,
GooglePlayApp => GooglePlayAppServices,
User => UserAppServices
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
      email = app.email,
      sessionToken = app.sessionToken)

  def fromUserApp(app: User): UserAppServices =
    UserAppServices(
      id = app.id,
      email = app.email,
      sessionToken = app.sessionToken)

  def toUpdateInstallationResponse(installation: InstallationServices): UpdateInstallationResponse =
    UpdateInstallationResponse(
      androidId = installation.androidId,
      deviceToken = installation.deviceToken)

}
