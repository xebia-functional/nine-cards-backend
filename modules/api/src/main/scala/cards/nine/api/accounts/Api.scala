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
package cards.nine.api.accounts

import cards.nine.api.NineCardsDirectives._
import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.accounts.messages._
import cards.nine.api.utils.AkkaHttpMarshallers._
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.domain.account.SessionToken
import cards.nine.processes._
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.NineCardsServices._

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server._

class AccountsApi(
  implicit
  config: NineCardsConfiguration,
  accountProcesses: AccountProcesses[NineCardsServices],
  executionContext: ExecutionContext
) extends SprayJsonSupport {

  import Converters._
  import Directives._
  import JsonFormats._

  lazy val route: Route =
    pathPrefix("installations")(installationsRoute) ~
      pathPrefix("login")(userRoute)

  private[this] lazy val userRoute: Route =
    pathEndOrSingleSlash {
      post {
        entity(as[ApiLoginRequest]) { request ⇒
          nineCardsDirectives.authenticateLoginRequest { sessionToken: SessionToken ⇒
            complete {
              accountProcesses
                .signUpUser(toLoginRequest(request, sessionToken))
                .map(toApiLoginResponse)
            }
          }
        }
      }
    }

  private[this] lazy val installationsRoute: Route =
    nineCardsDirectives.authenticateUser { implicit userContext: UserContext ⇒
      pathEndOrSingleSlash {
        put {
          entity(as[ApiUpdateInstallationRequest]) { request ⇒
            complete {
              accountProcesses
                .updateInstallation(toUpdateInstallationRequest(request, userContext))
                .map(toApiUpdateInstallationResponse)
            }
          }
        }
      }
    }

}
