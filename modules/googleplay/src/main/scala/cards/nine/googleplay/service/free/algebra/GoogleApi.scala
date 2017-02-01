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
package cards.nine.googleplay.service.free.algebra

import cards.nine.domain.application.{ FullCard, Package, BasicCard }
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.domain._
import cards.nine.googleplay.domain.apigoogle._
import cats.free.{ Free, Inject }

object GoogleApi {

  sealed trait Ops[A]

  case class GetBulkDetails(packagesName: List[Package], marketAuth: MarketCredentials)
    extends Ops[Failure Either List[BasicCard]]

  case class GetDetails(packageName: Package, marketAuth: MarketCredentials)
    extends Ops[Failure Either FullCard]

  case class GetDetailsList(packages: List[Package], marketAuth: MarketCredentials)
    extends Ops[List[Failure Either FullCard]]

  case class RecommendationsByApps(request: RecommendByAppsRequest, auth: MarketCredentials)
    extends Ops[List[Package]]

  case class RecommendationsByCategory(request: RecommendByCategoryRequest, auth: MarketCredentials)
    extends Ops[InfoError Either List[Package]]

  case class SearchApps(request: SearchAppsRequest, auth: MarketCredentials)
    extends Ops[Failure Either List[Package]]

  class Services[F[_]](implicit inj: Inject[Ops, F]) {

    def getBulkDetails(
      packagesName: List[Package],
      auth: MarketCredentials
    ): Free[F, Failure Either List[BasicCard]] =
      Free.inject[Ops, F](GetBulkDetails(packagesName, auth))

    def getDetails(packageName: Package, auth: MarketCredentials): Free[F, Failure Either FullCard] =
      Free.inject[Ops, F](GetDetails(packageName, auth))

    def getDetailsList(packages: List[Package], auth: MarketCredentials): Free[F, List[Failure Either FullCard]] =
      Free.inject[Ops, F](GetDetailsList(packages, auth))

    def recommendationsByApps(request: RecommendByAppsRequest, auth: MarketCredentials): Free[F, List[Package]] =
      Free.inject[Ops, F](RecommendationsByApps(request, auth))

    def recommendationsByCategory(request: RecommendByCategoryRequest, auth: MarketCredentials): Free[F, InfoError Either List[Package]] =
      Free.inject[Ops, F](RecommendationsByCategory(request, auth))

    def searchApps(request: SearchAppsRequest, auth: MarketCredentials): Free[F, Failure Either List[Package]] =
      Free.inject[Ops, F](SearchApps(request, auth))
  }

  object Services {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Services[F] = new Services[F]
  }

}
