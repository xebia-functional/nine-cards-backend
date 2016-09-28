package cards.nine.googleplay.api

import cards.nine.googleplay.domain._
import spray.http.StatusCodes.Unauthorized
import spray.routing.{Directive, Directives, MissingHeaderRejection, RejectionHandler }

object Headers {
  val token = "X-Google-Play-Token"
  val androidId = "X-Android-ID"
  val localization = "X-Android-Market-Localization"
}

object CustomDirectives {
  import Directives._
  import shapeless._

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

  val priceFilterPath: Directive[PriceFilter :: HNil] =
    path(CustomMatchers.PriceFilterSegment) |
    (pathEndOrSingleSlash & provide(PriceFilter.ALL: PriceFilter) )


}

object AuthHeadersRejectionHandler {

  implicit val authHeadersRejectionHandler = RejectionHandler {
    case MissingHeaderRejection(headerName: String) :: _ ⇒
      Directives.complete(Unauthorized, "Missing authorization headers needed for this request")
  }

}

