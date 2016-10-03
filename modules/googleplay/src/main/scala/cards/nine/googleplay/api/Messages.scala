package cards.nine.googleplay.api

import cards.nine.googleplay.domain.Package

case class ApiCard(
  packageName: String,
  title: String,
  free: Boolean,
  icon: String,
  stars: Double,
  downloads: String,
  categories: List[String]
)

case class ApiCardList(
  missing: List[String],
  apps: List[ApiCard]
)

case class ApiRecommendation (
  packageName: String,
  title: String,
  free: Boolean,
  icon: String,
  stars: Double,
  downloads: String,
  screenshots: List[String]
)

case class ApiRecommendationList(apps: List[ApiRecommendation])

case class ApiRecommendByAppsRequest(
  searchByApps: List[Package],
  numPerApp: Int,
  excludedApps: List[Package],
  maxTotal: Int
)

case class ApiRecommendByCategoryRequest(
  excludedApps: List[Package],
  maxTotal: Int
)
