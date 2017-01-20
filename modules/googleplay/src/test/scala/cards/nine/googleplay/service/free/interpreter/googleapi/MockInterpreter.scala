package cards.nine.googleplay.service.free.interpreter.googleapi

import cards.nine.domain.application.{ BasicCard, FullCard, Package }
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.domain._
import cards.nine.googleplay.domain.apigoogle.Failure
import cards.nine.googleplay.service.free.algebra.GoogleApi._
import cats.~>

trait InterpreterServer[F[_]] {
  def getDetails(pack: Package, auth: MarketCredentials): F[Failure Either FullCard]
  def getDetailsList(packageNames: List[Package], marketAuth: MarketCredentials): F[List[Failure Either FullCard]]
  def getBulkDetails(packageNames: List[Package], marketAuth: MarketCredentials): F[Failure Either List[BasicCard]]
  def recommendationsByApps(request: RecommendByAppsRequest, auth: MarketCredentials): F[List[Package]]
  def recommendationsByCategory(request: RecommendByCategoryRequest, auth: MarketCredentials): F[InfoError Either List[Package]]
  def searchApps(request: SearchAppsRequest, auth: MarketCredentials): F[Failure Either List[Package]]
}

case class MockInterpreter[F[_]](server: InterpreterServer[F]) extends (Ops ~> F) {

  override def apply[A](ops: Ops[A]) = ops match {
    case GetDetails(pack, auth) ⇒ server.getDetails(pack, auth)
    case GetDetailsList(packs, auth) ⇒ server.getDetailsList(packs, auth)
    case GetBulkDetails(packs, auth) ⇒ server.getBulkDetails(packs, auth)
    case RecommendationsByApps(request, auth) ⇒ server.recommendationsByApps(request, auth)
    case RecommendationsByCategory(request, auth) ⇒ server.recommendationsByCategory(request, auth)
    case SearchApps(request, auth) ⇒ server.searchApps(request, auth)
  }

}

