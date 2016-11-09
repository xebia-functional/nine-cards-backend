package cards.nine.services.free.interpreter.googleplay

import cards.nine.commons.NineCardsErrors.{ PackageNotResolved, RecommendationsServerError }
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.TaskInstances._
import cards.nine.domain.application._
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.processes.Wiring.GooglePlayApp
import cards.nine.googleplay.processes.{ CardsProcesses, Wiring }
import cards.nine.services.free.algebra.GooglePlay._
import cats.syntax.either._
import cats.~>

import scalaz.concurrent.Task

class Services(implicit googlePlayProcesses: CardsProcesses[GooglePlayApp]) extends (Ops ~> Task) {

  def resolveOne(packageName: Package, auth: MarketCredentials): Task[Result[FullCard]] = {
    googlePlayProcesses.getCard(packageName, auth)
      .foldMap(Wiring.interpreters).map { result ⇒
        result
          .leftMap(e ⇒ PackageNotResolved(e.packageName.value))
          .toEither
      }
  }

  def resolveManyBasic(packages: List[Package], auth: MarketCredentials): Task[Result[CardList[BasicCard]]] =
    googlePlayProcesses.getBasicCards(packages, auth)
      .map(r ⇒ Either.right(Converters.toCardList(r)))
      .foldMap(Wiring.interpreters)

  def resolveManyDetailed(packages: List[Package], auth: MarketCredentials): Task[Result[CardList[FullCard]]] =
    googlePlayProcesses.getCards(packages, auth)
      .map(r ⇒ Either.right(Converters.toCardList(r)))
      .foldMap(Wiring.interpreters)

  def recommendByCategory(
    category: String,
    filter: PriceFilter,
    excludedPackages: List[Package],
    limit: Int,
    auth: MarketCredentials
  ): Task[Result[CardList[FullCard]]] =
    googlePlayProcesses.recommendationsByCategory(
      Converters.toRecommendByCategoryRequest(category, filter, excludedPackages, limit),
      auth
    ).foldMap(Wiring.interpreters)
      .map(
        _.bimap(
        e ⇒ RecommendationsServerError(e.message),
        r ⇒ Converters.omitMissing(r)
      ).toEither
      )

  def recommendationsForApps(
    packageNames: List[Package],
    excludedPackages: List[Package],
    limitByApp: Int,
    limit: Int,
    auth: MarketCredentials
  ): Task[Result[CardList[FullCard]]] =
    googlePlayProcesses.recommendationsByApps(
      Converters.toRecommendByAppsRequest(packageNames, limitByApp, excludedPackages, limit),
      auth
    ).map(r ⇒ Either.right(Converters.omitMissing(r)))
      .foldMap(Wiring.interpreters)

  def searchApps(
    query: String,
    excludePackages: List[Package],
    limit: Int,
    auth: MarketCredentials
  ): Task[Result[CardList[BasicCard]]] =
    googlePlayProcesses.searchApps(
      Converters.toSearchAppsRequest(query, excludePackages, limit),
      auth
    )
      .map(r ⇒ Either.right(Converters.omitMissing(r)))
      .foldMap(Wiring.interpreters)

  def resolvePendingApps(numPackages: Int): Task[ResolvePendingStats] =
    googlePlayProcesses
      .resolvePendingApps(numPackages)
      .map(Converters.toResolvePendingStats)
      .foldMap(Wiring.interpreters)

  def apply[A](fa: Ops[A]): Task[A] = fa match {
    case ResolveManyBasic(packageNames, auth) ⇒
      resolveManyBasic(packageNames, auth)
    case ResolveManyDetailed(packageNames, auth) ⇒
      resolveManyDetailed(packageNames, auth)
    case Resolve(packageName, auth) ⇒
      resolveOne(packageName, auth)
    case RecommendationsByCategory(category, filter, excludesPackages, limit, auth) ⇒
      recommendByCategory(category, filter, excludesPackages, limit, auth)
    case RecommendationsForApps(packagesName, excludesPackages, limitPerApp, limit, auth) ⇒
      recommendationsForApps(packagesName, excludesPackages, limitPerApp, limit, auth)
    case SearchApps(query, excludePackages, limit, auth) ⇒
      searchApps(query, excludePackages, limit, auth)
    case ResolvePendingApps(numPackages) ⇒
      resolvePendingApps(numPackages)

  }
}

object Services {

  def services(implicit googlePlayProcesses: CardsProcesses[GooglePlayApp]) = new Services
}
