package cards.nine.services.free.interpreter.googleplay

import cards.nine.commons.TaskInstances._
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.domain.Package
import cards.nine.googleplay.processes.Wiring.GooglePlayApp
import cards.nine.googleplay.processes.{ CardsProcesses, Wiring }
import cards.nine.services.free.algebra.GooglePlay._
import cards.nine.services.free.domain.GooglePlay._
import cats.data.Xor
import cats.~>

import scalaz.concurrent.Task

class Services(implicit googlePlayProcesses: CardsProcesses[GooglePlayApp]) extends (Ops ~> Task) {

  def resolveOne(packageName: String, auth: MarketCredentials): Task[String Xor AppInfo] = {
    googlePlayProcesses.getCard(
      pack = Package(packageName),
      auth = auth
    ).foldMap(Wiring.interpreters).map {
        _.bimap(e ⇒ e.packageName.value, c ⇒ Converters.toAppInfo(c))
      }
  }

  def resolveMany(
    packageNames: List[String],
    auth: MarketCredentials,
    extendedInfo: Boolean
  ): Task[AppsInfo] = {
    val packages = packageNames map Package

    if (extendedInfo)
      googlePlayProcesses.getCards(packages, auth)
        .foldMap(Wiring.interpreters)
        .map(Converters.toAppsInfo)
    else
      googlePlayProcesses.getBasicCards(packages, auth)
        .foldMap(Wiring.interpreters)
        .map(Converters.toAppsInfo)
  }

  def recommendByCategory(
    category: String,
    filter: String,
    excludedPackages: List[String],
    limit: Int,
    auth: MarketCredentials
  ): Task[Recommendations] =
    googlePlayProcesses.recommendationsByCategory(
      Converters.toRecommendByCategoryRequest(category, filter, excludedPackages, limit),
      auth
    ).foldMap(Wiring.interpreters).flatMap {
        case Xor.Right(rec) ⇒ Task.delay(Converters.toRecommendations(rec))
        case Xor.Left(e) ⇒ Task.fail(new RuntimeException(e.message))
      }

  def recommendationsForApps(
    packageNames: List[String],
    excludedPackages: List[String],
    limitByApp: Int,
    limit: Int,
    auth: MarketCredentials
  ): Task[Recommendations] =
    googlePlayProcesses.recommendationsByApps(
      Converters.toRecommendByAppsRequest(packageNames, limitByApp, excludedPackages, limit),
      auth
    ).foldMap(Wiring.interpreters).map(Converters.toRecommendations)

  def searchApps(
    query: String,
    excludePackages: List[String],
    limit: Int,
    auth: MarketCredentials
  ): Task[Recommendations] =
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
