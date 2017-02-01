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

class ApplicationProcesses[F[_]](implicit services: GooglePlay.Services[F]) {

  import Converters._

  def getAppsInfo(
    packagesName: List[Package],
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[FullCard]] =
    if (packagesName.isEmpty)
      NineCardsService.right(CardList(Nil, Nil, Nil))
    else
      services.resolveManyDetailed(
        packageNames = packagesName,
        auth         = marketAuth
      ) map filterCategorized

  def getAppsBasicInfo(
    packagesName: List[Package],
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[BasicCard]] =
    if (packagesName.isEmpty)
      NineCardsService.right(CardList(Nil, Nil, Nil))
    else
      services.resolveManyBasic(packagesName, marketAuth)

  def resolvePendingApps(numPackages: Int): NineCardsService[F, ResolvePendingStats] =
    services.resolvePendingApps(numPackages)

  def storeCard(card: FullCard): NineCardsService[F, Unit] =
    services.storeCard(card)

  def getRecommendationsByCategory(
    category: String,
    filter: PriceFilter,
    excludePackages: List[Package],
    limit: Int,
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[FullCard]] =
    services.recommendByCategory(
      category         = category,
      priceFilter      = filter,
      excludesPackages = excludePackages,
      limit            = limit,
      auth             = marketAuth
    )

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
      services.recommendationsForApps(
        packagesName     = packagesName,
        excludesPackages = excludedPackages,
        limitPerApp      = limitPerApp,
        limit            = limit,
        auth             = marketAuth
      )

  def searchApps(
    query: String,
    excludePackages: List[Package],
    limit: Int,
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[BasicCard]] =
    services.searchApps(
      query            = query,
      excludesPackages = excludePackages,
      limit            = limit,
      auth             = marketAuth
    )
}

object ApplicationProcesses {

  implicit def applicationProcesses[F[_]](implicit services: GooglePlay.Services[F]) =
    new ApplicationProcesses

}
