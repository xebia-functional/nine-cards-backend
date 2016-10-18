package cards.nine.api.messages

import cards.nine.domain.application.Package
import cards.nine.processes.messages.ApplicationMessages._
import cards.nine.processes.messages.RecommendationsMessages._

object GooglePlayMessages {

  case class CategorizedApp(packageName: Package, category: String)

  case class ApiGetAppsInfoRequest(items: List[Package])

  case class ApiCategorizeAppsResponse(errors: List[Package], items: List[CategorizedApp])

  case class ApiDetailAppsResponse(errors: List[Package], items: List[AppGooglePlayInfo])

  case class ApiGetRecommendationsByCategoryRequest(excludePackages: List[Package], limit: Int)

  case class ApiGetRecommendationsForAppsRequest(
    packages: List[Package],
    excludePackages: List[Package],
    limitPerApp: Option[Int],
    limit: Int
  )

  case class ApiGetRecommendationsResponse(items: List[GooglePlayRecommendation])

  case class ApiRankAppsRequest(location: Option[String], items: Map[String, List[Package]])

  case class ApiRankAppsResponse(items: Map[String, List[Package]])

  case class ApiSearchAppsRequest(
    query: String,
    excludePackages: List[Package],
    limit: Int
  )

  case class ApiSearchAppsResponse(items: List[GooglePlayRecommendation])

}
