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
package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.application.{ BasicCard, CardList, FullCard, Package, PriceFilter, ResolvePendingStats }
import cards.nine.domain.market.MarketCredentials
import freestyle._

@free trait GooglePlay {
  def resolve(pack: Package, auth: MarketCredentials): FS[Result[FullCard]]
  def resolveManyBasic(packs: List[Package], auth: MarketCredentials): FS[Result[CardList[BasicCard]]]
  def resolveManyDetailed(packs: List[Package], auth: MarketCredentials): FS[Result[CardList[FullCard]]]
  def recommendByCategory(
    category: String,
    priceFilter: PriceFilter,
    excludesPackages: List[Package],
    limit: Int,
    auth: MarketCredentials
  ): FS[Result[CardList[FullCard]]]
  def recommendationsForApps(
    packagesName: List[Package],
    excludesPackages: List[Package],
    limitPerApp: Option[Int],
    limit: Int,
    auth: MarketCredentials
  ): FS[Result[CardList[FullCard]]]
  def searchApps(
    query: String,
    excludesPackages: List[Package],
    limit: Int,
    auth: MarketCredentials
  ): FS[Result[CardList[BasicCard]]]
  def resolvePendingApps(numPackages: Int): FS[Result[ResolvePendingStats]]
  def storeCard(card: FullCard): FS[Result[Unit]]
}
