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
import cards.nine.googleplay.domain.apigoogle.{ Failure ⇒ ApiFailure }
import cards.nine.googleplay.domain.webscrapper.{ Failure ⇒ WebFailure }
import freestyle._

@free trait Cache {
  def getValid(pack: Package): FS[Option[FullCard]]
  def getValidMany(packages: List[Package]): FS[List[FullCard]]
  def putResolved(card: FullCard): FS[Unit]
  def putResolvedMany(cards: List[FullCard]): FS[Unit]
  def putPermanent(card: FullCard): FS[Unit]
  def setToPending(pack: Package): FS[Unit]
  def setToPendingMany(packs: List[Package]): FS[Unit]
  def addError(pack: Package): FS[Unit]
  def addErrorMany(packs: List[Package]): FS[Unit]
  def listPending(limit: Int): FS[List[Package]]
}

@free trait GoogleApi {
  def getBulkDetails(packagesName: List[Package], auth: MarketCredentials): FS[ApiFailure Either List[BasicCard]]
  def getDetails(packageName: Package, auth: MarketCredentials): FS[ApiFailure Either FullCard]
  def getDetailsList(packages: List[Package], auth: MarketCredentials): FS[List[ApiFailure Either FullCard]]
  def recommendationsByApps(request: RecommendByAppsRequest, auth: MarketCredentials): FS[List[Package]]
  def recommendationsByCategory(request: RecommendByCategoryRequest, auth: MarketCredentials): FS[InfoError Either List[Package]]
  def searchApps(request: SearchAppsRequest, auth: MarketCredentials): FS[ApiFailure Either List[Package]]
}

@free trait WebScraper {
  def existsApp(pack: Package): FS[Boolean]
  def getDetails(pack: Package): FS[WebFailure Either FullCard]
}
