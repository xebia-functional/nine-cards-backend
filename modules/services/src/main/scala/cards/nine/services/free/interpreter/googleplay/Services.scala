package cards.nine.services.free.interpreter.googleplay

import cards.nine.commons.TaskInstances._
import cards.nine.domain.application.{ FullCard, FullCardList, Package, PriceFilter }
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.processes.Wiring.GooglePlayApp
import cards.nine.googleplay.processes.{ CardsProcesses, Wiring }
import cards.nine.services.free.algebra.GooglePlay._
import cats.data.Xor
import cats.~>

import scalaz.concurrent.Task

class Services(implicit googlePlayProcesses: CardsProcesses[GooglePlayApp]) extends (Ops ~> Task) {

  def resolveOne(packageName: Package, auth: MarketCredentials): Task[String Xor FullCard] = {
    googlePlayProcesses.getCard(packageName, auth)
      .foldMap(Wiring.interpreters).map {
        _.bimap(e ⇒ e.packageName.value, c ⇒ c)
      }
  }

  def resolveMany(
    packages: List[Package],
    auth: MarketCredentials,
    extendedInfo: Boolean
  ): Task[FullCardList] = {
    if (extendedInfo)
      googlePlayProcesses.getCards(packages, auth)
        .foldMap(Wiring.interpreters)
        .map(Converters.toFullCardList)
    else
      googlePlayProcesses.getBasicCards(packages, auth)
        .foldMap(Wiring.interpreters)
        .map(Converters.toFullCardList)
  }

  def recommendByCategory(
    category: String,
    filter: PriceFilter,
    excludedPackages: List[Package],
    limit: Int,
    auth: MarketCredentials
  ): Task[FullCardList] =
    googlePlayProcesses.recommendationsByCategory(
      Converters.toRecommendByCategoryRequest(category, filter, excludedPackages, limit),
      auth
    ).foldMap(Wiring.interpreters).flatMap {
        case Xor.Right(rec) ⇒ Task.delay(Converters.toRecommendations(rec))
        case Xor.Left(e) ⇒ Task.fail(new RuntimeException(e.message))
      }

  def recommendationsForApps(
    packageNames: List[Package],
    excludedPackages: List[Package],
    limitByApp: Int,
    limit: Int,
    auth: MarketCredentials
  ): Task[FullCardList] =
    googlePlayProcesses.recommendationsByApps(
      Converters.toRecommendByAppsRequest(packageNames, limitByApp, excludedPackages, limit),
      auth
    ).foldMap(Wiring.interpreters).map(Converters.toRecommendations)

  def searchApps(
    query: String,
    excludePackages: List[Package],
    limit: Int,
    auth: MarketCredentials
  ): Task[FullCardList] =
    googlePlayProcesses.searchApps(
      Converters.toSearchAppsRequest(query, excludePackages, limit),
      auth
    ).foldMap(Wiring.interpreters).map(Converters.toRecommendations)

  def apply[A](fa: Ops[A]): Task[A] = fa match {
    case ResolveMany(packageNames, auth, basicInfo) ⇒
      resolveMany(packageNames, auth, basicInfo)
    case Resolve(packageName, auth) ⇒
      resolveOne(packageName, auth)
    case RecommendationsByCategory(category, filter, excludesPackages, limit, auth) ⇒
      recommendByCategory(category, filter, excludesPackages, limit, auth)
    case RecommendationsForApps(packagesName, excludesPackages, limitPerApp, limit, auth) ⇒
      recommendationsForApps(packagesName, excludesPackages, limitPerApp, limit, auth)
    case SearchApps(query, excludePackages, limit, auth) ⇒
      searchApps(query, excludePackages, limit, auth)
  }
}

object Services {

  def services(implicit googlePlayProcesses: CardsProcesses[GooglePlayApp]) = new Services
}
