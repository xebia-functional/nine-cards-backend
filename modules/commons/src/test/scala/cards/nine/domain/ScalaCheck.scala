package cards.nine.domain

import cards.nine.domain.application.{ Category, Moment, Package, PriceFilter }
import enumeratum.{ Enum, EnumEntry }
import org.scalacheck.{ Arbitrary, Gen }

object ScalaCheck {

  def arbEnumeratum[C <: EnumEntry](e: Enum[C]): Arbitrary[C] = Arbitrary(Gen.oneOf(e.values))

  implicit val arbCategory: Arbitrary[Category] = arbEnumeratum[Category](Category)

  implicit val arbMoment: Arbitrary[Moment] = arbEnumeratum[Moment](Moment)

  implicit val arbPackage: Arbitrary[Package] = Arbitrary {
    Gen.listOfN(16, Gen.alphaNumChar) map (l ⇒ Package(l.mkString))
  }

  implicit val arbPriceFilter: Arbitrary[PriceFilter] = arbEnumeratum[PriceFilter](PriceFilter)
}

