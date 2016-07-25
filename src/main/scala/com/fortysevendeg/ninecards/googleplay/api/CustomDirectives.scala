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

  val requestHeaders: Directive[ GoogleAuthParams :: HNil] =
    for {
      token        <- headerValueByName(Headers.token)
      androidId    <- headerValueByName(Headers.androidId)
      localization <- optionalHeaderValueByName(Headers.localization)
      authParams = GoogleAuthParams( AndroidId(androidId), Token(token), localization.map(Localization.apply) )

    } yield authParams :: HNil

  val packageParameters: Directive[PackageList :: HNil] = {
    def getPackages(params: Seq[(String,String)]): Seq[String] = params.collect {
      case ("id",x) => x
    }
    parameterSeq.hmap {
      case params :: HNil => PackageList(getPackages(params).toList)
    }
  }

}
