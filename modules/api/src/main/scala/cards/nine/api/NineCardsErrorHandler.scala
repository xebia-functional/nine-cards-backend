package cards.nine.api

import cards.nine.commons.NineCardsErrors._
import spray.http.StatusCodes._
import spray.http.{ HttpEntity, HttpResponse }
import spray.httpx.marshalling.ToResponseMarshallingContext

class NineCardsErrorHandler {

  def handleNineCardsErrors(e: NineCardsError, ctx: ToResponseMarshallingContext): Unit = {
    val (statusCode, errorMessage) = e match {
      case CountryNotFound(message) ⇒
        (NotFound, message)
      case GoogleAnalyticsServerError(message) ⇒
        (ServiceUnavailable, message)
      case HttpBadRequest(message) ⇒
        (BadRequest, message)
      case HttpNotFound(message) ⇒
        (NotFound, message)
      case HttpUnauthorized(message) ⇒
        (Unauthorized, message)
      case RankingNotFound(message) ⇒
        (NotFound, message)
      case ReportNotFound(message) ⇒
        (NotFound, message)
      case GoogleOAuthError(message) ⇒
        (Unauthorized, message)
    }

    ctx.marshalTo(HttpResponse(status = statusCode, entity = HttpEntity(errorMessage)))
  }
}

object NineCardsErrorHandler {
  implicit val handler: NineCardsErrorHandler = new NineCardsErrorHandler
}
