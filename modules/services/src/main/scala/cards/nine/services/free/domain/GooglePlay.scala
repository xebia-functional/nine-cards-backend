package cards.nine.services.free.domain

import cards.nine.domain.application.Package

object GooglePlay {

  case class AppsInfo(
    missing: List[Package],
    apps: List[AppInfo]
  )

  case class AppInfo(
    packageName: Package,
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    categories: List[String]
  )

  case class Recommendations(
    apps: List[Recommendation]
  )

  case class Recommendation(
    packageName: Package,
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    screenshots: List[String]
  )

  case class RecommendByCategoryRequest(
    excludedApps: List[Package],
    maxTotal: Int
  )

  case class RecommendationsForAppsRequest(
    searchByApps: List[Package],
    numPerApp: Int,
    excludedApps: List[Package],
    maxTotal: Int
  )
}

