package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.processes.ProcessesExceptions.SharedCollectionNotFoundException
import com.fortysevendeg.ninecards.services.persistence.PersistenceExceptions.PersistenceException
import spray.http.StatusCodes._
import spray.routing.{ ExceptionHandler, HttpService }
import spray.util.LoggingContext

trait NineCardsExceptionHandler extends HttpService {
  implicit def exceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: java.net.ConnectException ⇒
        requestUri {
          uri ⇒
            log.warning("Request to {} could not be handled normally", uri)
            complete(ServiceUnavailable, e.getMessage)
        }
      case e: PersistenceException ⇒
        requestUri {
          uri ⇒
            log.warning("Request to {} could not be handled normally", uri)
            complete(InternalServerError, e.getMessage)
        }
      case e: SharedCollectionNotFoundException ⇒
        requestUri {
          uri ⇒
            log.warning("Shared collection not found: {}", uri)
            complete(NotFound, e.getMessage)
        }
      case e: Throwable ⇒
        requestUri {
          uri ⇒
            log.warning("Request to {} could not be handled normally: {}", uri, e.getMessage)
            complete(InternalServerError, e.getMessage)
        }
    }
}