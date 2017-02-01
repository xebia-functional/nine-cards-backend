/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.services.free.interpreter.googleplay

import cards.nine.commons.NineCardsErrors.{ PackageNotResolved, RecommendationsServerError }
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.catscalaz.TaskInstances._
import cards.nine.domain.application._
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.processes.CardsProcesses
import cards.nine.services.free.algebra.GooglePlay._
import cats.syntax.either._
import cats.~>
import cats.free.Free

import scalaz.concurrent.Task

class Services[F[_]](
  implicit
  googlePlayProcesses: CardsProcesses[F],
  interpreter: F ~> Task
) extends (Ops ~> Task) {

  def resolveOne(packageName: Package, auth: MarketCredentials): Free[F, Result[FullCard]] =
    googlePlayProcesses.getCard(packageName, auth)
      .map(result ⇒ result.leftMap(e ⇒ PackageNotResolved(e.packageName.value)))

  def resolveManyBasic(packages: List[Package], auth: MarketCredentials): Free[F, Result[CardList[BasicCard]]] =
    googlePlayProcesses.getBasicCards(packages, auth)
      .map(r ⇒ Either.right(Converters.toCardList(r)))

  def resolveManyDetailed(packages: List[Package], auth: MarketCredentials): Free[F, Result[CardList[FullCard]]] =
    googlePlayProcesses.getCards(packages, auth)
      .map(r ⇒ Either.right(Converters.toCardList(r)))

  def recommendByCategory(
    category: String,
    filter: PriceFilter,
    excludedPackages: List[Package],
    limit: Int,
    auth: MarketCredentials
  ): Free[F, Result[CardList[FullCard]]] = {
    val request = Converters.toRecommendByCategoryRequest(category, filter, excludedPackages, limit)
    googlePlayProcesses.recommendationsByCategory(request, auth)
      .map(
        _.bimap(
          e ⇒ RecommendationsServerError(e.message),
          r ⇒ Converters.omitMissing(r)
        )
      )
  }

  def recommendationsForApps(
    packageNames: List[Package],
    excludedPackages: List[Package],
    limitByApp: Option[Int],
    limit: Int,
    auth: MarketCredentials
  ): Free[F, Result[CardList[FullCard]]] = {
    val request = Converters.toRecommendByAppsRequest(packageNames, limitByApp, excludedPackages, limit)
    googlePlayProcesses.recommendationsByApps(request, auth)
      .map(r ⇒ Either.right(Converters.omitMissing(r)))
  }

  def searchApps(
    query: String,
    excludePackages: List[Package],
    limit: Int,
    auth: MarketCredentials
  ): Free[F, Result[CardList[BasicCard]]] = {
    val request = Converters.toSearchAppsRequest(query, excludePackages, limit)
    googlePlayProcesses.searchApps(request, auth)
      .map(r ⇒ Either.right(Converters.omitMissing(r)))
  }

  def resolvePendingApps(numPackages: Int): Free[F, Result[ResolvePendingStats]] =
    googlePlayProcesses
      .resolvePendingApps(numPackages)
      .map(r ⇒ Either.right(Converters.toResolvePendingStats(r)))

  def storeCard(card: FullCard): Free[F, Result[Unit]] =
    googlePlayProcesses.storeCard(card)
      .map(Either.right)

  private[this] def applyFree[A](fa: Ops[A]): Free[F, A] = fa match {
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
    case StoreCard(card) ⇒
      storeCard(card)

  }

  def apply[A](fa: Ops[A]): Task[A] = applyFree[A](fa).foldMap(interpreter)

}

object Services {

  def services[F[_]](implicit googlePlayProcesses: CardsProcesses[F], interpret: F ~> Task) = new Services
}
