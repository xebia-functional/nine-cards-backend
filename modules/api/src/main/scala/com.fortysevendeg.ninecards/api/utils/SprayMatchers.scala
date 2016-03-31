package com.fortysevendeg.ninecards.api.utils

import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain.{ApiKey, PublicIdentifier}
import shapeless.HNil
import spray.http.Uri.Path
import spray.routing.PathMatcher.{Matched, Unmatched}
import spray.routing._

object SprayMatchers {

  object ApiKeySegment extends TypedSegmentMatcher[ApiKey](ApiKey.apply)

  object PublicIdentifierSegment extends TypedSegmentMatcher[PublicIdentifier](PublicIdentifier.apply)

  abstract class TypedSegmentMatcher[T](f: String => T) extends PathMatcher1[T] {

    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) => Matched(tail, f(segment) :: HNil)
      case _ => Unmatched
    }
  }

}
