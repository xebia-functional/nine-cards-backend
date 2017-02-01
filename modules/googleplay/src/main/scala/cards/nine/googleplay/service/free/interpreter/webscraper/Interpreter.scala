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
package cards.nine.googleplay.service.free.interpreter.webscrapper

import cards.nine.commons.config.Domain.GooglePlayWebConfiguration
import cats.syntax.either._
import cats.~>
import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.googleplay.domain.webscrapper._
import cards.nine.googleplay.service.free.algebra.WebScraper._
import org.http4s.Http4s._
import org.http4s.client.{ Client, UnexpectedStatus }
import org.http4s.{ Method, Request, Status, Uri }

import scalaz.concurrent.Task
import scodec.bits.ByteVector

class Interpreter(config: GooglePlayWebConfiguration) extends (Ops ~> WithClient) {

  override def apply[A](ops: Ops[A]): WithClient[A] = ops match {
    case ExistsApp(pack) ⇒ new ExistsAppWP(pack)
    case GetDetails(pack) ⇒ new GetDetailsWP(pack)
  }

  private[this] val baseUri = Uri(
    scheme    = Option(config.protocol.ci),
    authority = Option(Uri.Authority(host = Uri.RegName(config.host), port = Some(config.port)))
  )

  private[this] def detailsUriOf(pack: Package) = baseUri
    .withPath(config.paths.details)
    .withQueryParam("id", pack.value)
    .withQueryParam("hl", "en_US")

  private[this] class ExistsAppWP(pack: Package) extends WithClient[Boolean] {
    override def apply(client: Client): Task[Boolean] = {
      val request = new Request(method = Method.HEAD, uri = detailsUriOf(pack))
      client.fetch(request)(resp ⇒ Task.now(resp.status.isSuccess))
    }
  }

  private[this] class GetDetailsWP(pack: Package) extends WithClient[Failure Either FullCard] {

    def handleUnexpected(e: UnexpectedStatus): Failure = e.status match {
      case Status.NotFound ⇒ PackageNotFound(pack)
      case _ ⇒ WebPageServerError
    }

    override def apply(client: Client): Task[Failure Either FullCard] = {
      val httpRequest = new Request(method = Method.GET, uri = detailsUriOf(pack))

      client.expect[ByteVector](httpRequest).map { bv ⇒
        Either.fromOption(
          GooglePlayPageParser.parseCard(bv),
          PageParseFailed(pack)
        )
      }.handle {
        case e: UnexpectedStatus ⇒ Either.left(handleUnexpected(e))
      }
    }
  }

}
