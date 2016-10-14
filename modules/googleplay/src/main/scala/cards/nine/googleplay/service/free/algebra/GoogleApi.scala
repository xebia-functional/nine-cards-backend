package cards.nine.googleplay.service.free.algebra

import cards.nine.googleplay.domain._
import cards.nine.googleplay.domain.apigoogle._
import cats.data.Xor
import cats.free.{ Free, Inject }

object GoogleApi {

  sealed trait Ops[A]

  case class GetBulkDetails(packagesName: List[Package], authParams: GoogleAuthParams)
    extends Ops[Failure Xor List[FullCard]]

  case class GetDetails(packageName: Package, authParams: GoogleAuthParams)
    extends Ops[Failure Xor FullCard]

  case class RecommendationsByApps(request: RecommendByAppsRequest, auth: GoogleAuthParams)
    extends Ops[List[Package]]

  case class RecommendationsByCategory(request: RecommendByCategoryRequest, auth: GoogleAuthParams)
    extends Ops[InfoError Xor List[Package]]

  case class SearchApps(request: SearchAppsRequest, auth: GoogleAuthParams)
    extends Ops[Failure Xor List[Package]]

  class Services[F[_]](implicit inj: Inject[Ops, F]) {

    def getBulkDetails(
      packagesName: List[Package],
      auth: GoogleAuthParams
    ): Free[F, Failure Xor List[FullCard]] =
      Free.inject[Ops, F](GetBulkDetails(packagesName, auth))

    def getDetails(packageName: Package, auth: GoogleAuthParams): Free[F, Failure Xor FullCard] =
      Free.inject[Ops, F](GetDetails(packageName, auth))

    def recommendationsByApps(request: RecommendByAppsRequest, auth: GoogleAuthParams): Free[F, List[Package]] =
      Free.inject[Ops, F](RecommendationsByApps(request, auth))

    def recommendationsByCategory(request: RecommendByCategoryRequest, auth: GoogleAuthParams): Free[F, InfoError Xor List[Package]] =
      Free.inject[Ops, F](RecommendationsByCategory(request, auth))

    def searchApps(request: SearchAppsRequest, auth: GoogleAuthParams): Free[F, Failure Xor List[Package]] =
      Free.inject[Ops, F](SearchApps(request, auth))
  }

  object Services {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Services[F] = new Services[F]
  }

}
