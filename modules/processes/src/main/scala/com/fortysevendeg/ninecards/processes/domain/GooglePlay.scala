package com.fortysevendeg.ninecards.processes.domain

object GooglePlay {

  case class AppInfo(
    packageName: String,
    appType: String,
    appCategory: String,
    numDownloads: String,
    starRating: Double,
    ratingCount: Int,
    commentCount: Int
  )

  case class AppsInfo(
    missing: Seq[String],
    apps: Seq[AppInfo]
  )

}