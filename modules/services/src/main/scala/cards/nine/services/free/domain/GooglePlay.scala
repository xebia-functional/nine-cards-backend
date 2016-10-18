package cards.nine.services.free.domain

import cards.nine.domain.application.Package

object GooglePlay {

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

