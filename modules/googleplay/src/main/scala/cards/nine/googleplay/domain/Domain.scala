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
  numPerApp: Int,
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