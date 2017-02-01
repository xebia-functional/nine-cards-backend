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
package cards.nine.app

import akka.actor.Actor
import cards.nine.api.{ AuthHeadersRejectionHandler, NineCardsExceptionHandler, NineCardsRoutes }
import cards.nine.commons.config.NineCardsConfig
import spray.routing.HttpService
import spray.routing.authentication.{ BasicAuth, BasicHttpAuthenticator, UserPass }

import scala.concurrent.ExecutionContext

class NineCardsApiActor
  extends Actor
  with AuthHeadersRejectionHandler
  with HttpService
  with NineCardsExceptionHandler {

  override val actorRefFactory = context

  implicit val executionContext: ExecutionContext = actorRefFactory.dispatcher
  implicit val config = NineCardsConfig.nineCardsConfiguration

  implicit val editorBasicAuth: BasicHttpAuthenticator[String] =
    BasicAuth(
      realm      = "App Card Curators",
      config     = NineCardsConfig.defaultConfig.config,
      createUser = (up: UserPass) ⇒ up.user
    )

  def receive = runRoute(new NineCardsRoutes().nineCardsRoutes)

}
