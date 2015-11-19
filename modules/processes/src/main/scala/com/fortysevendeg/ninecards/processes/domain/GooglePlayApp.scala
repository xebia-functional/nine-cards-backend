package com.fortysevendeg.ninecards.processes.domain

case class CategorizeResponse(
  categorizedApps: Seq[GooglePlayApp],
  notFoundApps: Seq[String])

case class GooglePlayApp(
  packageName: String,
  appType: String,
  appCategory: String,
  numDownloads: String,
  starRating: Double,
  ratingCount: Int,
  commentCount: Int)
