package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.processes.ProcessesExceptions.SharedCollectionNotFoundException
import com.fortysevendeg.ninecards.services.persistence.PersistenceExceptions.PersistenceException
import org.http4s.client.UnexpectedStatus
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
            complete(ServiceUnavailable, Option(e.getMessage).getOrElse("Net connection error"))
        }
      case e: PersistenceException ⇒
        requestUri {
          uri ⇒
            log.warning("Request to {} could not be handled normally", uri)
            complete(InternalServerError, Option(e.getMessage).getOrElse("Persistence error"))
        }
      case e: SharedCollectionNotFoundException ⇒
        requestUri {
          uri ⇒
            log.warning("Shared collection not found: {}", uri)
            complete(NotFound, e.getMessage)
        }
      case e: messages.rankings.Reload.InvalidDate ⇒
        requestUri { uri ⇒
          log.warning("Request to {} could not be handled normally", uri)
          complete(BadRequest, e.getMessage)
        }
      case e: messages.rankings.Reload.Error ⇒
        requestUri { uri ⇒
          log.warning("Request to {} could not be handled normally", uri)
          val status = if (e.code == 401) Unauthorized else InternalServerError
          complete(status, e.message)
        }
      case e: UnexpectedStatus ⇒
        requestUri {
          uri ⇒
            log.warning("Request to {} could not be handled normally: {}", uri, e.status.toString)
            complete(InternalServerError, e.status.toString)
        }
      case e: Throwable ⇒
        requestUri {
          uri ⇒
            val exceptionMessage = Option(e.getMessage).getOrElse("Unexpected error")
            log.warning("Request to {} could not be handled normally: {}", uri, exceptionMessage)
            complete(InternalServerError, exceptionMessage)
        }
    }
}