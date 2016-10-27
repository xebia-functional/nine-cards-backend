package cards.nine.api

import cards.nine.commons.NineCardsErrors._
import spray.http.{ HttpEntity, HttpResponse, StatusCodes }
import spray.httpx.marshalling.ToResponseMarshallingContext

class NineCardsErrorHandler {

  def handleNineCardsErrors(e: NineCardsError, ctx: ToResponseMarshallingContext): Unit = e match {
    case CountryNotFound(message) ⇒
      ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound, entity = HttpEntity(message)))
    case GoogleAnalyticsServerError(message) ⇒
      ctx.marshalTo(HttpResponse(status = StatusCodes.ServiceUnavailable, entity = HttpEntity(message)))
    case HttpBadRequest(message) ⇒
      ctx.marshalTo(HttpResponse(status = StatusCodes.BadRequest, entity = HttpEntity(message)))
    case HttpNotFound(message) ⇒
      ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound, entity = HttpEntity(message)))
    case HttpUnauthorized(message) ⇒
      ctx.marshalTo(HttpResponse(status = StatusCodes.Unauthorized, entity = HttpEntity(message)))
    case RankingNotFound(message) ⇒
      ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound, entity = HttpEntity(message)))
    case ReportNotFound(message) ⇒
      ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound, entity = HttpEntity(message)))
  }
}

object NineCardsErrorHandler {
  implicit val handler: NineCardsErrorHandler = new NineCardsErrorHandler
}
