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
package cards.nine.googleplay.domain

import cards.nine.domain.application.{ Category, Package, PriceFilter }
import cards.nine.domain.market.MarketCredentials

case class AppRequest(
  packageName: Package,
  marketAuth: MarketCredentials
)

case class PackageList(items: List[Package]) extends AnyVal

case class InfoError(message: String) extends AnyVal

case class RecommendByAppsRequest(
  searchByApps: List[Package],
  numPerApp: Option[Int],
  excludedApps: List[Package],
  maxTotal: Int
)

case class RecommendByCategoryRequest(
  category: Category,
  priceFilter: PriceFilter,
  excludedApps: List[Package],
  maxTotal: Int
)

case class SearchAppsRequest(
  word: String,
  excludedApps: List[Package],
  maxTotal: Int
)