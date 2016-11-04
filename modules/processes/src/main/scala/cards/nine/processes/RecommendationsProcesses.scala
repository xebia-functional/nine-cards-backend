package cards.nine.processes

import cards.nine.domain.application.{ BasicCardList, FullCardList, Package, PriceFilter }
import cards.nine.domain.market.MarketCredentials
import cards.nine.services.free.algebra.GooglePlay
import cats.free.Free

class RecommendationsProcesses[F[_]](implicit services: GooglePlay.Services[F]) {

  def getRecommendationsByCategory(
    category: String,
    filter: PriceFilter,
    excludePackages: List[Package],
    limit: Int,
    marketAuth: MarketCredentials
  ): Free[F, FullCardList] =
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
  ): Free[F, FullCardList] =
    if (packagesName.isEmpty)
      Free.pure(FullCardList(Nil, Nil))
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
  ): Free[F, BasicCardList] =
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
