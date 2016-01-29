package com.fortysevendeg.ninecards.processes.converters

import com.fortysevendeg.ninecards.processes.domain._
import com.fortysevendeg.ninecards.processes.messages._
import com.fortysevendeg.ninecards.services.free.domain.{
GooglePlayApp => GooglePlayAppServices,
User => UserAppServices,
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
      sessionToken = app.sessionToken)

  def fromUserApp(app: User): UserAppServices =
    UserAppServices(
      sessionToken = app.sessionToken)

  def toInstallationRequestProcess(app: InstallationRequest): InstallationServices =
    InstallationServices(
      deviceToken = app.deviceToken,
      userId = app.userId
    )

  def fromInstallationProcesses(app: InstallationServices): Installation =
    Installation(
      id = app.id,
      deviceToken = app.deviceToken,
      userId = app.userId
    )

}
