package com.fortysevendeg.ninecards.api.utils

import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain.{ApiKey, PublicIdentifier}
import shapeless._
import spray.http.Uri.Path
import spray.routing.PathMatcher.{Matched, Unmatched}
import spray.routing._

object SprayMatchers extends ImplicitPathMatcherConstruction {

  object ApiKeySegment extends TypedSegment[ApiKey]

  object PublicIdentifierSegment extends TypedSegment[PublicIdentifier]

  abstract class TypedSegment[T](implicit gen: Generic.Aux[T, String :: HNil]) extends PathMatcher1[T] {
    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) => Matched(tail, gen.from(segment :: HNil) :: HNil)
      case _ => Unmatched
    }
  }

}
