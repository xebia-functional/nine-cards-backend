package cards.nine.processes

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.application.{ BasicCard, CardList, FullCard, Package, PriceFilter }
import cards.nine.domain.market.MarketCredentials
import cards.nine.services.free.algebra.GooglePlay

class RecommendationsProcesses[F[_]](implicit services: GooglePlay.Services[F]) {

  def getRecommendationsByCategory(
    category: String,
    filter: PriceFilter,
    excludePackages: List[Package],
    limit: Int,
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[FullCard]] =
    services.recommendByCategory(
      category         = category,
      priceFilter      = filter,
      excludesPackages = excludePackages,
      limit            = limit,
      auth             = marketAuth
    )

  def getRecommendationsForApps(
    packagesName: List[Package],
    excludedPackages: List[Package],
    limitPerApp: Int,
    limit: Int,
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[FullCard]] =
    if (packagesName.isEmpty)
      NineCardsService.right(CardList(Nil, Nil))
    else
      services.recommendationsForApps(
        packagesName     = packagesName,
        excludesPackages = excludedPackages,
        limitPerApp      = limitPerApp,
        limit            = limit,
        auth             = marketAuth
      )

  def searchApps(
    query: String,
    excludePackages: List[Package],
    limit: Int,
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[BasicCard]] =
    services.searchApps(
      query            = query,
      excludesPackages = excludePackages,
      limit            = limit,
      auth             = marketAuth
    )
}

object RecommendationsProcesses {

  implicit def recommendationsProcesses[F[_]](implicit services: GooglePlay.Services[F]) =
    new RecommendationsProcesses

}
