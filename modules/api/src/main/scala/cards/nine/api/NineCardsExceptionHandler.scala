/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.api

import akka.event.LoggingAdapter
import org.http4s.client.UnexpectedStatus
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{ Directives, ExceptionHandler }

trait NineCardsExceptionHandler extends Directives {
  implicit def exceptionHandler(implicit log: LoggingAdapter) =
    ExceptionHandler {
      case e: java.net.ConnectException ⇒
        extractUri {
          uri ⇒
            log.warning("Request to {} could not be handled normally", uri)
            complete(ServiceUnavailable, Option(e.getMessage).getOrElse("Net connection error"))
        }
      case e: rankings.messages.Reload.InvalidDate ⇒
        extractUri { uri ⇒
          log.warning("Request to {} could not be handled normally", uri)
          complete(BadRequest, e.getMessage())
        }
      case e: rankings.messages.Reload.Error ⇒
        extractUri { uri ⇒
          log.warning("Request to {} could not be handled normally", uri)
          val status = if (e.code == 401) Unauthorized else InternalServerError
          complete(status, e.message)
        }
      case e: UnexpectedStatus ⇒
        extractUri {
          uri ⇒
            log.warning("Request to {} could not be handled normally: {}", uri, e.status.toString)
            complete(InternalServerError, e.status.toString)
        }
      case e: Throwable ⇒
        extractUri {
          uri ⇒
            val exceptionMessage = Option(e.getMessage).getOrElse("Unexpected error")
            log.warning("Request to {} could not be handled normally: {}", uri, exceptionMessage)
            complete(InternalServerError, exceptionMessage)
        }
    }
}