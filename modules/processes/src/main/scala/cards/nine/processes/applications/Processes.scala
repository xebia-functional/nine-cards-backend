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
package cards.nine.processes.applications

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.application.{ BasicCard, CardList, FullCard, Package, PriceFilter }
import cards.nine.domain.application.{ ResolvePendingStats }
import cards.nine.domain.market.MarketCredentials
import cards.nine.services.free.algebra.GooglePlay

class ApplicationProcesses[F[_]](implicit services: GooglePlay[F]) {

  import Converters._

  def toNCS[A](fs: freestyle.FreeS.Par[F, Result[A]]): NineCardsService[F, A] = NineCardsService[F, A](fs.monad)

  def getAppsInfo(
    packs: List[Package],
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[FullCard]] =
    if (packs.isEmpty)
      NineCardsService.right(CardList(Nil, Nil, Nil))
    else
      toNCS(services.resolveManyDetailed(packs, marketAuth)).map(filterCategorized)

  def getAppsBasicInfo(
    packs: List[Package],
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[BasicCard]] =
    if (packs.isEmpty)
      NineCardsService.right(CardList(Nil, Nil, Nil))
    else
      toNCS(services.resolveManyBasic(packs, marketAuth))

  def resolvePendingApps(numPackages: Int): NineCardsService[F, ResolvePendingStats] =
    toNCS(services.resolvePendingApps(numPackages))

  def storeCard(card: FullCard): NineCardsService[F, Unit] =
    toNCS(services.storeCard(card))

  def getRecommendationsByCategory(
    category: String,
    filter: PriceFilter,
    excludePackages: List[Package],
    limit: Int,
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[FullCard]] =
    toNCS(services.recommendByCategory(
      category         = category,
      priceFilter      = filter,
      excludesPackages = excludePackages,
      limit            = limit,
      auth             = marketAuth
    ))

  def getRecommendationsForApps(
    packagesName: List[Package],
    excludedPackages: List[Package],
    limitPerApp: Option[Int],
    limit: Int,
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[FullCard]] =
    if (packagesName.isEmpty)
      NineCardsService.right(CardList(Nil, Nil, Nil))
    else
      toNCS(services.recommendationsForApps(
        packagesName     = packagesName,
        excludesPackages = excludedPackages,
        limitPerApp      = limitPerApp,
        limit            = limit,
        auth             = marketAuth
      ))

  def searchApps(
    query: String,
    excludePackages: List[Package],
    limit: Int,
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[BasicCard]] =
    toNCS(services.searchApps(
      query            = query,
      excludesPackages = excludePackages,
      limit            = limit,
      auth             = marketAuth
    ))
}

object ApplicationProcesses {

  implicit def applicationProcesses[F[_]](implicit services: GooglePlay[F]) = new ApplicationProcesses

}

