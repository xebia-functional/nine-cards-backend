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

import cats.syntax.either._
import cards.nine.domain.application.FullCard
import cards.nine.googleplay.domain._
import org.http4s.{ Method, Request, Uri }
import org.http4s.Status.ResponseClass.Successful
import org.http4s.client.Client
import scalaz.concurrent.Task
import scodec.bits.ByteVector

class Http4sGooglePlayWebScraper(serverUrl: String, client: Client) {

  private[this] def buildRequest(appRequest: AppRequest): Option[Request] = {
    val packageName: String = appRequest.packageName.value
    val locale = appRequest.marketAuth.localization.fold("")(l ⇒ s"&hl=${l.value}")
    val uriString = s"${serverUrl}?id=${packageName}${locale}"

    for /*Option*/ {
      uri ← Uri.fromString(uriString).toOption
      request = new Request(method = Method.GET, uri = uri)
    } yield request
  }

  private[this] def runRequest[L, R](
    appRequest: AppRequest,
    failed: ⇒ L,
    parserR: ByteVector ⇒ Either[L, R]
  ): Task[Either[L, R]] = {
    lazy val leftFailed = Either.left(failed)
    buildRequest(appRequest) match {
      case Some(request) ⇒
        client.fetch(request) {
          case Successful(resp) ⇒
            resp.as[ByteVector].map(parserR)
          case _ ⇒
            Task.now(Either.left(failed))
        }.handle {
          case _ ⇒ Either.left(failed)
        }

      case None ⇒ Task.now(leftFailed)
    }
  }

  def getCard(appRequest: AppRequest): Task[InfoError Either FullCard] = {
    lazy val failed: InfoError = InfoError(appRequest.packageName.value)
    runRequest[InfoError, FullCard](appRequest, failed, { bv ⇒
      Either.fromOption(
        GooglePlayPageParser.parseCard(bv),
        failed
      )
    })
  }
}
