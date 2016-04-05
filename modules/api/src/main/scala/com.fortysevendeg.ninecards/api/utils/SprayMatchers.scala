package com.fortysevendeg.ninecards.api.utils

import shapeless._
import spray.http.Uri.Path
import spray.routing.PathMatcher.{Matched, Unmatched}
import spray.routing._

object SprayMatchers {

  class TypedSegment[T](implicit gen: Generic.Aux[T, String :: HNil]) extends PathMatcher1[T] {
    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) ⇒ Matched(tail, gen.from(segment :: HNil) :: HNil)
      case _                           ⇒ Unmatched
    }
  }

  object TypedSegment {
    def apply[T](implicit gen: Generic.Aux[T, String :: HNil]) = new TypedSegment[T]
  }

}
