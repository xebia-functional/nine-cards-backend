package com.fortysevendeg.ninecards.googleplay.api

import com.fortysevendeg.ninecards.googleplay.domain._

object Converters {

  def toApiCard( fullCard: FullCard) : ApiCard =
    ApiCard(
      packageName = fullCard.packageName,
      title = fullCard.title,
      free = fullCard.free,
      icon = fullCard.icon,
      categories = fullCard.categories,
      stars = fullCard.stars,
      downloads  = fullCard.downloads
    )

  def toApiCardList(fullCards: FullCardList): ApiCardList =
    ApiCardList(
      fullCards.missing,
      fullCards.cards map toApiCard
    )

  def toApiRecommendation(fullCard: FullCard) : ApiRecommendation =
    ApiRecommendation(
      packageName = fullCard.packageName,
      title = fullCard.title,
      free = fullCard.free,
      icon = fullCard.icon,
      stars = fullCard.stars,
      downloads  = fullCard.downloads,
      screenshots = fullCard.screenshots
    )

  def toApiRecommendationList(fullCards: FullCardList): ApiRecommendationList =
    ApiRecommendationList(fullCards.cards map toApiRecommendation)

  def toRecommendByAppsRequest(api: ApiRecommendByAppsRequest): RecommendByAppsRequest =
    RecommendByAppsRequest(
      searchByApps = api.searchByApps,
      excludedApps = api.excludedApps,
      numPerApp = api.numPerApp,
      maxTotal = api.maxTotal)

  def toRecommendByCategoryRequest(
    category: Category,
    priceFilter: PriceFilter,
    api: ApiRecommendByCategoryRequest) : RecommendByCategoryRequest =
    RecommendByCategoryRequest(
      category = category,
      priceFilter = priceFilter,
      excludedApps = api.excludedApps,
      maxTotal = api.maxTotal
    )
}
