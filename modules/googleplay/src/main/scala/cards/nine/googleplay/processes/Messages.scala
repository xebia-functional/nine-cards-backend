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
package cards.nine.googleplay.processes

import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.domain.market.MarketCredentials

package object getcard {
  sealed trait FailedResponse { val packageName: Package }
  case class WrongAuthParams(packageName: Package, marketAuth: MarketCredentials) extends FailedResponse
  case class PendingResolution(packageName: Package) extends FailedResponse
  case class UnknownPackage(packageName: Package) extends FailedResponse
  type Response = FailedResponse Either FullCard
}

object ResolveMany {
  case class Response[A](notFound: List[Package], pending: List[Package], apps: List[A])
}

object ResolvePending {

  case class Response(solved: List[Package], unknown: List[Package], pending: List[Package])

  sealed trait PackageStatus
  case object Resolved extends PackageStatus
  case object Pending extends PackageStatus
  case object Unknown extends PackageStatus
}

