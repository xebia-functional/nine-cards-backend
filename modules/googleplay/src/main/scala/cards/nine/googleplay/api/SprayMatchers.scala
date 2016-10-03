package cards.nine.googleplay.api

import cards.nine.googleplay.domain.{Category, PriceFilter}
import enumeratum.{ Enum, EnumEntry }
import shapeless._
import spray.http.Uri.Path
import spray.routing.PathMatcher.{ Matched, Unmatched }
import spray.routing._

object CustomMatchers {

  class EnumSegment[E <: EnumEntry](implicit En: Enum[E]) extends PathMatcher1[E] {
    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) ⇒ En.withNameOption(segment) match {
        case Some(e) => Matched(tail, e :: HNil)
        case None => Unmatched
      }
      case _ ⇒ Unmatched
    }
  }

  val CategorySegment: PathMatcher1[Category] = new EnumSegment[Category]

  val PriceFilterSegment: PathMatcher1[PriceFilter] = new EnumSegment[PriceFilter]

}
