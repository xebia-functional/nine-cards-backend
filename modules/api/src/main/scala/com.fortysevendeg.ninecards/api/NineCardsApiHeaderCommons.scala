package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.api.NineCardsHeaders._
import shapeless.HList._
import shapeless.HNil
import spray.http.StatusCodes
import spray.routing.directives.HeaderDirectives._
import spray.routing.{ HttpService, MissingHeaderRejection, RejectionHandler }

object NineCardsApiHeaderCommons {

  def requestLoginHeaders = for {
    appId ← headerValueByName(headerAppId)
    apiKey ← headerValueByName(headerApiKey)
  } yield appId :: apiKey :: HNil

}

trait AuthHeadersRejectionHandler extends HttpService {
  implicit val authHeadersRejectionHandler = RejectionHandler {
    case MissingHeaderRejection(headerName: String) :: _ ⇒
      complete(StatusCodes.Unauthorized, "Missing authorization headers needed for this request")
  }
}