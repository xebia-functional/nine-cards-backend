package cards.nine.api

import cards.nine.commons.NineCardsErrors._
import spray.http.{ HttpResponse, StatusCodes }
import spray.httpx.marshalling.ToResponseMarshallingContext

class NineCardsErrorHandler {

  def handleNineCardsErrors(e: NineCardsError, ctx: ToResponseMarshallingContext): Unit = e match {
    case AuthTokenNotValid(message) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.Unauthorized))
    case CountryNotFound(message) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound))
    case FirebaseServerError(message) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.ServiceUnavailable))
    case GoogleAnalyticsServerError(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.ServiceUnavailable))
    case HttpBadRequest(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.BadRequest))
    case HttpNotFound(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound))
    case HttpUnauthorized(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.Unauthorized))
    case InstallationNotFound(message) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.Unauthorized))
    case RankingNotFound(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound))
    case ReportNotFound(_) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.NotFound))
    case UserNotFound(message) ⇒ ctx.marshalTo(HttpResponse(status = StatusCodes.Unauthorized))
  }
}

object NineCardsErrorHandler {
  implicit val handler: NineCardsErrorHandler = new NineCardsErrorHandler
}
