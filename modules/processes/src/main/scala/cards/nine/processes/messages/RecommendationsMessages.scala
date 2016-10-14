package cards.nine.processes.messages

import cards.nine.domain.application.Package

object RecommendationsMessages {

  case class GetRecommendationsResponse(items: List[GooglePlayRecommendation])

  case class GooglePlayRecommendation(
    packageName: Package,
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    screenshots: List[String]
  )

  case class RecommendationAppList(apps: List[GooglePlayRecommendation])

  type SearchAppsResponse = RecommendationAppList

}
