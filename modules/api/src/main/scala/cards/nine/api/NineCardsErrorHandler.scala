package cards.nine.api

import cards.nine.commons.NineCardsErrors._
import spray.http.{ HttpResponse, StatusCodes }
import spray.httpx.marshalling.ToResponseMarshallingContext

class NineCardsErrorHandler {

  def handleNineCardsErrors(e: NineCardsError, ctx: ToResponseMarshallingContext): Unit = e match {
    case CountryNotFound(message) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound))
    case GoogleAnalyticsServerError(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.ServiceUnavailable))
    case HttpBadRequest(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.BadRequest))
    case HttpNotFound(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound))
    case HttpUnauthorized(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.Unauthorized))
    case RankingNotFound(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound))
    case ReportNotFound(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound))
  }
}

object NineCardsErrorHandler {
  implicit val handler: NineCardsErrorHandler = new NineCardsErrorHandler
}
