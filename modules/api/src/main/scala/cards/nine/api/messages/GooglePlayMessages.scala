package cards.nine.api.messages

import cards.nine.domain.application.{ Package, Widget }

object GooglePlayMessages {

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

  case class ApiRankByMomentsRequest(
    location: Option[String],
    items: List[Package],
    moments: List[String],
    limit: Int
  )

  case class ApiRankedWidgetsByMoment(moment: String, widgets: List[Widget])

  case class ApiRankWidgetsResponse(items: List[ApiRankedWidgetsByMoment])

}
