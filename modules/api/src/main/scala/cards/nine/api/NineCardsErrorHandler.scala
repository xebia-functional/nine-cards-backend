package cards.nine.api

import cards.nine.commons.NineCardsErrors.{ CountryNotFound, NineCardsError }
import spray.http.{ HttpResponse, StatusCodes }
import spray.httpx.marshalling.ToResponseMarshallingContext

class NineCardsErrorHandler {

  def handleNineCardsErrors(e: NineCardsError, ctx: ToResponseMarshallingContext): Unit = e match {
    case CountryNotFound(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound))
  }
}

object NineCardsErrorHandler {
  implicit val handler: NineCardsErrorHandler = new NineCardsErrorHandler
}
