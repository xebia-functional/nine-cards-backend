package cards.nine.services.free.domain

object GooglePlay {

  case class PackageList(items: List[String]) extends AnyVal

  case class AuthParams(
    androidId: String,
    localization: Option[String],
    token: String
  )

  case class AppsInfo(
    missing: List[String],
    apps: List[AppInfo]
  )

  case class AppInfo(
    packageName: String,
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
    packageName: String,
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    screenshots: List[String]
  )

  case class RecommendByCategoryRequest(
    excludedApps: List[String],
    maxTotal: Int
  )

  case class RecommendationsForAppsRequest(
    searchByApps: List[String],
    numPerApp: Int,
    excludedApps: List[String],
    maxTotal: Int
  )
}

