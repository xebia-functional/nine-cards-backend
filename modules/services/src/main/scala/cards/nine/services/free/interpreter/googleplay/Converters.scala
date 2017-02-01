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

import cards.nine.domain.application._
import cards.nine.googleplay.domain._
import cards.nine.googleplay.processes.{ getcard, ResolveMany, ResolvePending }
import cats.instances.either._
import cats.instances.list._
import cats.syntax.monadCombine._

object Converters {

  def omitMissing[A](cardsList: CardList[A]): CardList[A] = cardsList.copy(missing = Nil)

  def toSearchAppsRequest(
    query: String,
    excludePackages: List[Package],
    limit: Int
  ): SearchAppsRequest = SearchAppsRequest(query, excludePackages, limit)

  def toRecommendByAppsRequest(
    packages: List[Package],
    limitByApp: Option[Int],
    excludedPackages: List[Package],
    limit: Int
  ): RecommendByAppsRequest =
    RecommendByAppsRequest(
      packages,
      limitByApp,
      excludedPackages,
      limit
    )

  def toRecommendByCategoryRequest(
    category: String,
    filter: PriceFilter,
    excludedPackages: List[Package],
    limit: Int
  ): RecommendByCategoryRequest =
    RecommendByCategoryRequest(
      Category.withName(category),
      filter,
      excludedPackages,
      limit
    )

  def toCardList[A](response: ResolveMany.Response[A]): CardList[A] =
    CardList(
      response.notFound,
      response.pending,
      response.apps
    )

  def toFullCardList(response: List[getcard.Response]): CardList[FullCard] = {
    val (errors, resolved) = response.separate
    CardList(
      errors map (_.packageName),
      Nil,
      resolved
    )
  }

  def toResolvePendingStats(response: ResolvePending.Response): ResolvePendingStats =
    ResolvePendingStats(
      resolved = response.solved.length,
      pending  = response.pending.length,
      errors   = response.unknown.length
    )

}
