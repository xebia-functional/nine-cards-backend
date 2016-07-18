package com.fortysevendeg.ninecards.services.free.domain

object GooglePlay {

  case class PackageList(items: Seq[String]) extends AnyVal

  case class AuthParams(
    androidId: String,
    localization: Option[String],
    token: String
  )

  case class AppsCards(
    unresolved: List[String],
    apps: List[AppCard]
  )

  case class AppCard(
    packageName: String,
    title: String,
    description: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    categories: List[String]
  )

  case class UnresolvedApp(
    packageName: String,
    cause: String
  )

}

