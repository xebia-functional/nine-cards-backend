package cards.nine.processes

import cards.nine.processes.converters.Converters._
import cards.nine.processes.messages.GooglePlayAuthMessages._
import cards.nine.processes.messages.RecommendationsMessages._
import cards.nine.services.free.algebra.GooglePlay
import cats.free.Free

class RecommendationsProcesses[F[_]](implicit services: GooglePlay.Services[F]) {

  def getRecommendationsByCategory(
    category: String,
    filter: String,
    excludePackages: List[String],
    limit: Int,
    authParams: AuthParams
  ): Free[F, GetRecommendationsResponse] =
    services.recommendByCategory(
      category         = category,
      priceFilter      = filter,
      excludesPackages = excludePackages,
      limit            = limit,
      auth             = toAuthParamsServices(authParams)
    ) map toGetRecommendationsResponse

  def getRecommendationsForApps(
    packagesName: List[String],
    excludedPackages: List[String],
    limitPerApp: Int,
    limit: Int,
    authParams: AuthParams
  ): Free[F, GetRecommendationsResponse] =
    if (packagesName.isEmpty)
      Free.pure(GetRecommendationsResponse(Nil))
    else
      services.recommendationsForApps(
        packagesName     = packagesName,
        excludesPackages = excludedPackages,
        limitPerApp      = limitPerApp,
        limit            = limit,
        auth             = toAuthParamsServices(authParams)
      ) map toGetRecommendationsResponse
}

object RecommendationsProcesses {

  implicit def recommendationsProcesses[F[_]](implicit services: GooglePlay.Services[F]) =
    new RecommendationsProcesses

}
