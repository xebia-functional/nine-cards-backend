package com.fortysevendeg.ninecards.api.messages

import com.fortysevendeg.ninecards.processes.messages.ApplicationMessages._
import com.fortysevendeg.ninecards.processes.messages.RecommendationsMessages._

object GooglePlayMessages {

  case class CategorizedApp(packageName: String, category: String)

  case class ApiGetAppsInfoRequest(items: List[String])

  case class ApiCategorizeAppsResponse(errors: List[String], items: List[CategorizedApp])

  case class ApiDetailAppsResponse(errors: List[String], items: List[AppGooglePlayInfo])

  case class ApiGetRecommendationsByCategoryRequest(excludePackages: List[String], limit: Int)

  case class ApiGetRecommendationsForAppsRequest(
    packages: List[String],
    excludePackages: List[String],
    limit: Int
  )

  case class ApiGetRecommendationsResponse(items: List[GooglePlayRecommendation])

  case class ApiRankAppsRequest(items: Map[String, List[String]])

  case class ApiRankAppsResponse(items: Map[String, List[String]])
}
