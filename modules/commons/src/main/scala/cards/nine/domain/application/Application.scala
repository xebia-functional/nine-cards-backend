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
package cards.nine.domain.application

import enumeratum.{ Enum, EnumEntry }
import scala.util.matching.Regex

/**
  * A Package is the unique identifier of an android App.
  * It is a dot-separated sequence of lowercase segments, much like Java packages.
  */
case class Package(value: String) extends AnyVal

object PackageRegex {

  val string: String = {
    val c = """[a-zA-Z0-9\_]+"""
    s"""$c(?:\\.$c)*"""
  }

  val regex: Regex = string.r

  def parse(str: String): Option[Package] =
    regex.unapplySeq(str) map (_ ⇒ Package(str))

}

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
  pending: List[Package],
  cards: List[A]
)

object FullCardList {
  def apply(missing: List[Package], pending: List[Package], cards: List[FullCard]) =
    CardList(missing, pending, cards)
}

sealed trait PriceFilter extends EnumEntry
object PriceFilter extends Enum[PriceFilter] {
  case object ALL extends PriceFilter
  case object FREE extends PriceFilter
  case object PAID extends PriceFilter

  val values = super.findValues
}

case class ResolvePendingStats(
  resolved: Int,
  pending: Int,
  errors: Int
)
