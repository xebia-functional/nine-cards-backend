package cards.nine.processes

import cards.nine.domain.market.MarketCredentials
import cards.nine.processes.converters.Converters._
import cards.nine.processes.messages.RecommendationsMessages._
import cards.nine.services.free.algebra.GooglePlay
import cats.free.Free

class RecommendationsProcesses[F[_]](implicit services: GooglePlay.Services[F]) {

  def getRecommendationsByCategory(
    category: String,
    filter: String,
    excludePackages: List[String],
    limit: Int,
    marketAuth: MarketCredentials
  ): Free[F, GetRecommendationsResponse] =
    services.recommendByCategory(
      category         = category,
      priceFilter      = filter,
      excludesPackages = excludePackages,
      limit            = limit,
      auth             = marketAuth
    ) map toGetRecommendationsResponse

  def getRecommendationsForApps(
    packagesName: List[String],
    excludedPackages: List[String],
    limitPerApp: Int,
    limit: Int,
    marketAuth: MarketCredentials
  ): Free[F, GetRecommendationsResponse] =
    if (packagesName.isEmpty)
      Free.pure(GetRecommendationsResponse(Nil))
    else
      services.recommendationsForApps(
        packagesName     = packagesName,
        excludesPackages = excludedPackages,
        limitPerApp      = limitPerApp,
        limit            = limit,
        auth             = marketAuth
      ) map toGetRecommendationsResponse

  def searchApps(
    query: String,
    excludePackages: List[String],
    limit: Int,
    marketAuth: MarketCredentials
  ): Free[F, SearchAppsResponse] =
    services.searchApps(
      query            = query,
      excludesPackages = excludePackages,
      limit            = limit,
      auth             = marketAuth
    ) map toSearchAppsResponse

}

object RecommendationsProcesses {

  implicit def recommendationsProcesses[F[_]](implicit services: GooglePlay.Services[F]) =
    new RecommendationsProcesses

}
