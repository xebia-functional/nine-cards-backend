package cards.nine.api

import spray.http.StatusCodes.Unauthorized
import spray.routing.{ Directives, MissingHeaderRejection, RejectionHandler }

trait AuthHeadersRejectionHandler {

  implicit val authHeadersRejectionHandler = RejectionHandler {
    case MissingHeaderRejection(headerName: String) :: _ ⇒
      Directives.complete(Unauthorized, "Missing authorization headers needed for this request")
  }

}