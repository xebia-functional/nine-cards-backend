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
import cards.nine.commons.NineCardsService._
import cards.nine.domain.oauth._
import cats.free.{ :<: }

object GoogleOAuth {

  sealed trait Ops[A]

  case class FetchAccessToken(credentials: ServiceAccount)
    extends Ops[Result[AccessToken]]

  class Services[F[_]](implicit I: Ops :<: F) {

    def fetchAcessToken(serviceAccount: ServiceAccount): NineCardsService[F, AccessToken] =
      NineCardsService(FetchAccessToken(serviceAccount))
  }

  object Services {
    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] = new Services
  }

}