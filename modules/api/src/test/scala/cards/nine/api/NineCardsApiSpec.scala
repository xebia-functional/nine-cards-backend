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

import akka.actor.ActorSystem
import akka.testkit._
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.commons.config.NineCardsConfig
import cards.nine.processes.NineCardsServices._
import cards.nine.processes._
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.applications.ApplicationProcesses
import cards.nine.processes.rankings.RankingProcesses
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.duration.DurationInt
import akka.http.scaladsl.model.StatusCodes.{ NotFound, OK }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ RouteTestTimeout, Specs2RouteTest }

class NineCardsApiSpec
  extends Specification
  with AuthHeadersRejectionHandler
  with JsonFormats
  with Matchers
  with Mockito
  with NineCardsExceptionHandler
  with Specs2RouteTest {

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(20.second dilated system)

  implicit def actorRefFactory = system

  implicit val accountProcesses: AccountProcesses[NineCardsServices] = mock[AccountProcesses[NineCardsServices]]

  implicit val applicationProcesses: ApplicationProcesses[NineCardsServices] = mock[ApplicationProcesses[NineCardsServices]]

  implicit val rankingProcesses: RankingProcesses[NineCardsServices] = mock[RankingProcesses[NineCardsServices]]

  implicit val config: NineCardsConfiguration = NineCardsConfig.nineCardsConfiguration

  val route = Route.seal(new NineCardsRoutes().nineCardsRoutes)

  "nineCardsApi" should {
    "grant access to HealthCheck" in {
      Get("/healthcheck") ~> route ~> check {
        status should be equalTo OK.intValue
      }
    }

    "return a 404 NotFound error for an undefined path " in {
      Get("/chalkyTown") ~> route ~> check {
        status.intValue shouldEqual NotFound.intValue
      }
    }

  }

}
