package cards.nine.googleplay.service.free.algebra

import cards.nine.domain.application.{ FullCard, Package, BasicCard }
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.domain._
import cards.nine.googleplay.domain.apigoogle._
import cats.data.Xor
import cats.free.{ Free, Inject }

object GoogleApi {

  sealed trait Ops[A]

  case class GetBulkDetails(packagesName: List[Package], marketAuth: MarketCredentials)
    extends Ops[Failure Xor List[BasicCard]]

  case class GetDetails(packageName: Package, marketAuth: MarketCredentials)
    extends Ops[Failure Xor FullCard]

  case class RecommendationsByApps(request: RecommendByAppsRequest, auth: MarketCredentials)
    extends Ops[List[Package]]

  case class RecommendationsByCategory(request: RecommendByCategoryRequest, auth: MarketCredentials)
    extends Ops[InfoError Xor List[Package]]

  case class SearchApps(request: SearchAppsRequest, auth: MarketCredentials)
    extends Ops[Failure Xor List[Package]]

  class Services[F[_]](implicit inj: Inject[Ops, F]) {

    def getBulkDetails(
      packagesName: List[Package],
      auth: MarketCredentials
    ): Free[F, Failure Xor List[BasicCard]] =
      Free.inject[Ops, F](GetBulkDetails(packagesName, auth))

    def getDetails(packageName: Package, auth: MarketCredentials): Free[F, Failure Xor FullCard] =
      Free.inject[Ops, F](GetDetails(packageName, auth))

    def recommendationsByApps(request: RecommendByAppsRequest, auth: MarketCredentials): Free[F, List[Package]] =
      Free.inject[Ops, F](RecommendationsByApps(request, auth))

    def recommendationsByCategory(request: RecommendByCategoryRequest, auth: MarketCredentials): Free[F, InfoError Xor List[Package]] =
      Free.inject[Ops, F](RecommendationsByCategory(request, auth))

    def searchApps(request: SearchAppsRequest, auth: MarketCredentials): Free[F, Failure Xor List[Package]] =
      Free.inject[Ops, F](SearchApps(request, auth))
  }

  object Services {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Services[F] = new Services[F]
  }

}
