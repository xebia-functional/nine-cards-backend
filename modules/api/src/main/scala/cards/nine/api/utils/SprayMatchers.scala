package cards.nine.api.utils

import cards.nine.api.messages.PathEnumerations.PriceFilter
import cards.nine.domain.analytics.{ Continent, Country }
import cards.nine.domain.application.Category
import enumeratum.{ Enum, EnumEntry }
import shapeless._
import spray.http.Uri.Path
import spray.routing.PathMatcher.{ Matched, Unmatched }
import spray.routing.PathMatchers.IntNumber
import spray.routing._

object SprayMatchers {

  class EnumSegment[E <: EnumEntry](implicit En: Enum[E]) extends PathMatcher1[E] {
    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) ⇒ En.withNameOption(segment) match {
        case Some(e) ⇒ Matched(tail, e :: HNil)
        case None ⇒ Unmatched
      }
      case _ ⇒ Unmatched
    }
  }

  val CategorySegment: PathMatcher1[Category] = new EnumSegment[Category]
  val ContinentSegment: PathMatcher1[Continent] = new EnumSegment[Continent]
  val CountrySegment: PathMatcher1[Country] = new EnumSegment[Country]
  val PriceFilterSegment: PathMatcher1[PriceFilter] = new EnumSegment[PriceFilter]

  class TypedSegment[T](implicit gen: Generic.Aux[T, String :: HNil]) extends PathMatcher1[T] {
    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) ⇒ Matched(tail, gen.from(segment :: HNil) :: HNil)
      case _ ⇒ Unmatched
    }
  }

  object TypedSegment {
    def apply[T](implicit gen: Generic.Aux[T, String :: HNil]) = new TypedSegment[T]
  }

  class TypedIntSegment[T](implicit gen: Generic.Aux[T, Int :: HNil]) extends PathMatcher1[T] {
    def apply(path: Path) = IntNumber.apply(path) map (segment ⇒ gen.from(segment) :: HNil)
  }

  object TypedIntSegment {
    def apply[T](implicit gen: Generic.Aux[T, Int :: HNil]) = new TypedIntSegment[T]
  }

}
