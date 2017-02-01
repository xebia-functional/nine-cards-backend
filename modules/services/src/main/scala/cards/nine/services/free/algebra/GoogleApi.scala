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
package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService.{ NineCardsService, Result }
import cards.nine.domain.account.GoogleIdToken
import cards.nine.services.free.domain.TokenInfo
import cats.free.:<:

object GoogleApi {

  sealed trait Ops[A]

  case class GetTokenInfo(tokenId: GoogleIdToken) extends Ops[Result[TokenInfo]]

  class Services[F[_]](implicit I: Ops :<: F) {

    def getTokenInfo(tokenId: GoogleIdToken): NineCardsService[F, TokenInfo] =
      NineCardsService(GetTokenInfo(tokenId))
  }

  object Services {

    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] = new Services
  }
}
