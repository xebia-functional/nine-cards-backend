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
}
