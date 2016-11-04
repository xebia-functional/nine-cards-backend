package cards.nine.domain.application

import enumeratum.{ Enum, EnumEntry }

/**
  * A Package is the unique identifier of an android App.
  * It is a dot-separated sequence of lowercase segments, much like Java packages.
  */
case class Package(value: String) extends AnyVal

case class Widget(packageName: Package, className: String)

object Widget {
  val FromRaw = "([a-zA-Z0-9\\.\\_]+):([a-zA-Z0-9\\.\\_]+)".r

  def apply(raw: String): Option[Widget] = raw match {
    case FromRaw(packageName, className) ⇒ Option(Widget(Package(packageName), className))
    case _ ⇒ None
  }
}

/**
  * A FullCard contains all the information about an existing Android App
  * managed by the NineCards Backend application.
  */
case class FullCard(
  packageName: Package,
  title: String,
  categories: List[String],
  downloads: String,
  free: Boolean,
  icon: String,
  screenshots: List[String],
  stars: Double
) {
  def toBasic: BasicCard = BasicCard(packageName, title, downloads, free, icon, stars)
}

/**
  *  A BasicCard only contains the most important information about an existing Android App.
  *  Unlike a FullCard, it lacks categories and screenshots.
  */
case class BasicCard(
  packageName: Package,
  title: String,
  downloads: String,
  free: Boolean,
  icon: String,
  stars: Double
)

/**
  * A CardList carries the information known in the backend about a set of packages.
  * The `cards` field contains the FullCard for those packages for which one is available.
  * The `missing` field contains the name of those packages for which there is none.a
  */
case class CardList[A](
  missing: List[Package],
  cards: List[A]
)

object BasicCardList {
  def apply(missing: List[Package], cards: List[BasicCard]) = CardList(missing, cards)
}
object FullCardList {
  def apply(missing: List[Package], cards: List[FullCard]) = CardList(missing, cards)
}

sealed trait PriceFilter extends EnumEntry
object PriceFilter extends Enum[PriceFilter] {
  case object ALL extends PriceFilter
  case object FREE extends PriceFilter
  case object PAID extends PriceFilter

  val values = super.findValues
}

