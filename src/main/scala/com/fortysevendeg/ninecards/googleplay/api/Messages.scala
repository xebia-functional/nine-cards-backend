package com.fortysevendeg.ninecards.googleplay.api

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

