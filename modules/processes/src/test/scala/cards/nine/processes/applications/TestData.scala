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

import cards.nine.domain.account.AndroidId
import cards.nine.domain.application.{ CardList, FullCard, Package, PriceFilter }
import cards.nine.domain.market.{ Localization, MarketCredentials, MarketToken }

private[applications] object TestData {

  val packagesName = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany",
    "earth.europe.france",
    "earth.europe.portugal",
    "earth.europe.spain"
  ) map Package

  val title = "Title of the app"

  val free = true

  val icon = "path-to-icon"

  val stars = 4.5

  val downloads = "1000000+"

  val category = "SOCIAL"

  val (missing, found) = packagesName.partition(_.value.length > 6)

  val apps = found map (packageName ⇒ FullCard(packageName, title, List(category), downloads, free, icon, Nil, stars))

  val marketAuth = {
    val androidId = "12345"
    val localization = "en_GB"
    val token = "m52_9876"
    MarketCredentials(AndroidId(androidId), MarketToken(token), Some(Localization(localization)))
  }

  val emptyGetAppsInfoResponse = CardList(Nil, Nil, Nil)

  val appsInfo = CardList(missing, Nil, apps)
}

object RecommendationsTestData {

  val excludePackages = TestData.packagesName.filter(_.value.length > 20)

  val screenshots = List("path-to-screenshot-1", "path-to-screenshot-2")

  val recommendedApps = TestData.packagesName map (packageName ⇒
    FullCard(packageName, TestData.title, Nil, TestData.downloads, TestData.free, TestData.icon, screenshots, TestData.stars))

  val limit = 20

  val limitPerApp = Some(100)

  val smallLimit = 1

  val recommendationFilter = PriceFilter.ALL

  val recommendations = CardList(Nil, Nil, recommendedApps)
}

object GooglePlayTestData {

  val (missing, foundPackageNames) = TestData.packagesName.partition(_.value.length < 20)

  val apps = foundPackageNames map { packageName ⇒
    FullCard(
      packageName = packageName,
      title       = "Title of app",
      free        = true,
      icon        = "Icon of app",
      stars       = 5.0,
      downloads   = "Downloads of app",
      screenshots = Nil,
      categories  = List("Country")
    )
  }

}
