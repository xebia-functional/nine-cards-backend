package cards.nine.api.applications

import cards.nine.domain.application.Package
import cards.nine.api.messages.GooglePlayMessages.ApiRecommendation

private[this] object messages {
  case class ApiCategorizedApp(
    packageName: Package,
    categories: List[String]
  )

  case class ApiIconApp(
    packageName: Package,
    title: String,
    icon: String
  )

  // ApiDetailsApp: FullCard without Screenshots
  case class ApiDetailsApp(
    packageName: Package,
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    categories: List[String]
  )

  case class ApiAppsInfoRequest(items: List[Package])
  case class ApiAppsInfoResponse[A](errors: List[Package], items: List[A])

  case class ApiRankAppsRequest(location: Option[String], items: Map[String, List[Package]])
  case class ApiRankAppsResponse(items: List[ApiRankedAppsByCategory])

  case class ApiRankedAppsByCategory(category: String, packages: List[Package])

  case class ApiSearchAppsRequest(
    query: String,
    excludePackages: List[Package],
    limit: Int
  )
  case class ApiSearchAppsResponse(items: List[ApiRecommendation])

  case class ApiSetAppInfoRequest(
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    categories: List[String],
    screenshots: List[String]
  )
  case class ApiSetAppInfoResponse()
}
