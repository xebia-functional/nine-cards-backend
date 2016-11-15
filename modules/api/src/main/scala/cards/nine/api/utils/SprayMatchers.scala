package cards.nine.api.utils

import cards.nine.domain.application.{ Category, Package, PackageRegex, PriceFilter }
import enumeratum.{ Enum, EnumEntry }
import shapeless._
import spray.http.Uri.Path
import spray.routing.PathMatcher.{ Matched, Unmatched }
import spray.routing.PathMatchers.{ IntNumber, Segment }
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

  val PackageSegment: PathMatcher1[Package] = Segment flatMap PackageRegex.parse

}
