package com.fortysevendeg.ninecards.googleplay.api

import com.fortysevendeg.ninecards.googleplay.domain._
import spray.routing.{Directive, Directives}
import shapeless._

object Headers {
  val token = "X-Google-Play-Token"
  val androidId = "X-Android-ID"
  val localization = "X-Android-Market-Localization"
}

object CustomDirectives {
  import Directives._

  val requestHeaders: Directive[ Token :: AndroidId :: Option[Localization] :: HNil] =
    for {
      token        <- headerValueByName(Headers.token)
      androidId    <- headerValueByName(Headers.androidId)
      localization <- optionalHeaderValueByName(Headers.localization)
    } yield Token(token) :: AndroidId(androidId) :: localization.map(Localization.apply) :: HNil

}
