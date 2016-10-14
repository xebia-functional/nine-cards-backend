package cards.nine.googleplay.domain

import cards.nine.domain.application.Category
import enumeratum.{ Enum, EnumEntry }

case class Package(value: String) extends AnyVal
case class AndroidId(value: String) extends AnyVal
case class Token(value: String) extends AnyVal
case class Localization(value: String) extends AnyVal

case class GoogleAuthParams(
  androidId: AndroidId,
  token: Token,
  localization: Option[Localization]
)

case class AppRequest(
  packageName: Package,
  authParams: GoogleAuthParams
)

case class PackageList(items: List[String]) extends AnyVal

case class InfoError(message: String) extends AnyVal

sealed trait PriceFilter extends EnumEntry
object PriceFilter extends Enum[PriceFilter] {
  case object ALL extends PriceFilter
  case object FREE extends PriceFilter
  case object PAID extends PriceFilter

  val values = super.findValues
}

case class FullCard(
  packageName: String,
  title: String,
  categories: List[String],
  downloads: String,
  free: Boolean,
  icon: String,
  screenshots: List[String],
  stars: Double
)

case class FullCardList(
  missing: List[String],
  cards: List[FullCard]
)

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
  excludedApps: List[String],
  maxTotal: Int
)

