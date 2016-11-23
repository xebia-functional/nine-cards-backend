package cards.nine.api.messages

import cards.nine.domain.application.{ Package, Widget }

object GooglePlayMessages {

  case class ApiCategorizedApp(
    packageName: Package,
    categories: List[String]
  )

  case class ApiIconApp(
    packageName: Package,
    title: String,
    icon: String
  )

  case class ApiAppsInfoRequest(items: List[Package])

  case class ApiAppsInfoResponse[A](errors: List[Package], items: List[A])

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

  case class ApiGetRecommendationsByCategoryRequest(excludePackages: List[Package], limit: Int)

  case class ApiGetRecommendationsForAppsRequest(
    packages: List[Package],
    excludePackages: List[Package],
    limitPerApp: Option[Int],
    limit: Int
  )

  case class ApiGetRecommendationsResponse(items: List[ApiRecommendation])

  // ApiRecommendation: FullCard without Categories
  case class ApiRecommendation(
    packageName: Package,
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    screenshots: List[String]
  )

  case class ApiRankAppsRequest(location: Option[String], items: Map[String, List[Package]])

  case class ApiRankByMomentsRequest(
    location: Option[String],
    items: List[Package],
    moments: List[String],
    limit: Int
  )

  case class ApiRankedAppsByCategory(category: String, packages: List[Package])

  case class ApiRankAppsResponse(items: List[ApiRankedAppsByCategory])

  case class ApiRankedWidgetsByMoment(moment: String, widgets: List[Widget])

  case class ApiRankWidgetsResponse(items: List[ApiRankedWidgetsByMoment])

  case class ApiSearchAppsRequest(
    query: String,
    excludePackages: List[Package],
    limit: Int
  )

  case class ApiSearchAppsResponse(items: List[ApiRecommendation])

}
