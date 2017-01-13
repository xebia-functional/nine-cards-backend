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

  val emptyGetAppsInfoResponse = CardList(Nil, Nil)

  val appsInfo = CardList(missing, apps)
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

  val recommendations = CardList(Nil, recommendedApps)
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
