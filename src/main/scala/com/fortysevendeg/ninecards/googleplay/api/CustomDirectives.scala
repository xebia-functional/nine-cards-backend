package com.fortysevendeg.ninecards.googleplay.api

import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._
import spray.routing.{Directive, Directives}
import shapeless._

object Headers {
  val token = "X-Google-Play-Token"
  val androidId = "X-Android-ID"
  val localisation = "X-Android-Market-Localization"
}

object CustomDirectives {
  import Directives._

  val requestHeaders: Directive[ Token :: AndroidId :: Option[Localization] :: HNil] =
    for {
      token        <- headerValueByName(Headers.token)
      androidId    <- headerValueByName(Headers.androidId)
      localisation <- optionalHeaderValueByName(Headers.localisation)
    } yield Token(token) :: AndroidId(androidId) :: localisation.map(Localization.apply) :: HNil

}
