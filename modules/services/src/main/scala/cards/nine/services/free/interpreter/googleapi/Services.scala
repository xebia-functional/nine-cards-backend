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
package cards.nine.services.free.interpreter.googleapi

import cards.nine.commons.NineCardsErrors.WrongGoogleAuthToken
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.config.Domain.GoogleApiConfiguration
import cards.nine.domain.account.GoogleIdToken
import cards.nine.services.free.algebra.GoogleApi._
import cards.nine.services.free.domain.{ TokenInfo, WrongTokenInfo }
import cats.syntax.either._
import cats.~>
import org.http4s.Http4s._
import org.http4s.Uri
import org.http4s.Uri.{ Authority, RegName }

import scalaz.concurrent.Task

class Services(config: GoogleApiConfiguration) extends (Ops ~> Task) {

  import Decoders._

  val client = org.http4s.client.blaze.PooledHttp1Client()

  def getTokenInfo(tokenId: GoogleIdToken): Task[Result[TokenInfo]] = {
    val authority = Authority(host = RegName(config.host), port = config.port)

    val getTokenInfoUri = Uri(scheme = Option(config.protocol.ci), authority = Option(authority))
      .withPath(config.tokenInfo.path)
      .withQueryParam(config.tokenInfo.tokenIdQueryParameter, tokenId.value)

    client
      .expect[WrongTokenInfo Either TokenInfo](getTokenInfoUri)
      .map { response ⇒
        response.leftMap(e ⇒ WrongGoogleAuthToken(e.error_description))
      }
  }

  def apply[A](fa: Ops[A]): Task[A] = fa match {
    case GetTokenInfo(tokenId: GoogleIdToken) ⇒ getTokenInfo(tokenId)
  }
}

object Services {

  implicit def services(config: GoogleApiConfiguration) = new Services(config)
}
