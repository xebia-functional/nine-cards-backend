package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.messages.GooglePlayAuthMessages._
import com.fortysevendeg.ninecards.processes.messages.RecommendationsMessages._
import com.fortysevendeg.ninecards.services.free.algebra.GooglePlay

class RecommendationsProcesses[F[_]](implicit services: GooglePlay.Services[F]) {

  def getRecommendationsByCategory(
    category: String,
    filter: String,
    excludePackages: List[String],
    limit: Int,
    authParams: AuthParams
  ): Free[F, GetRecommendationsResponse] =
    services.recommendByCategory(
      category    = category,
      priceFilter = filter,
      auth        = toAuthParamsServices(authParams)
    ) map { recommendations ⇒
        GetRecommendationsResponse(
          recommendations.apps
            .filterNot(r ⇒ excludePackages.contains(r.packageName))
            .take(limit)
            .map(toGooglePlayRecommendation)
        )
      }
}

object RecommendationsProcesses {

  implicit def recommendationsProcesses[F[_]](implicit services: GooglePlay.Services[F]) =
    new RecommendationsProcesses

}
