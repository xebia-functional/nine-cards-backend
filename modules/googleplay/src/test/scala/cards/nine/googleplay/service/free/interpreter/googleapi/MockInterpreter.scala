package cards.nine.googleplay.service.free.interpreter.googleapi

import cats.~>
import cats.data.Xor
import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.domain._
import cards.nine.googleplay.domain.apigoogle.{ Failure }
import cards.nine.googleplay.service.free.algebra.GoogleApi._

trait InterpreterServer[F[_]] {
  def getDetails(pack: Package, auth: MarketCredentials): F[Failure Xor FullCard]
  def getBulkDetails(packageNames: List[Package], marketAuth: MarketCredentials): F[Failure Xor List[FullCard]]
  def recommendationsByApps(request: RecommendByAppsRequest, auth: MarketCredentials): F[List[Package]]
  def recommendationsByCategory(request: RecommendByCategoryRequest, auth: MarketCredentials): F[InfoError Xor List[Package]]
  def searchApps(request: SearchAppsRequest, auth: MarketCredentials): F[Failure Xor List[Package]]
}

case class MockInterpreter[F[_]](server: InterpreterServer[F]) extends (Ops ~> F) {

  override def apply[A](ops: Ops[A]) = ops match {
    case GetDetails(pack, auth) ⇒ server.getDetails(pack, auth)
    case GetBulkDetails(packs, auth) ⇒ server.getBulkDetails(packs, auth)
    case RecommendationsByApps(request, auth) ⇒ server.recommendationsByApps(request, auth)
    case RecommendationsByCategory(request, auth) ⇒ server.recommendationsByCategory(request, auth)
    case SearchApps(request, auth) ⇒ server.searchApps(request, auth)
  }

}

