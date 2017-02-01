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
package cards.nine.googleplay.service.free.interpreter.googleapi

import akka.actor.ActorSystem
import cards.nine.commons.catscalaz.TaskInstances._
import cards.nine.commons.config.NineCardsConfig.nineCardsConfiguration
import cards.nine.domain.application.FullCard
import cards.nine.googleplay.config.TestConfig._
import cards.nine.googleplay.domain._
import cards.nine.googleplay.processes.GooglePlayApp.GooglePlayApp
import cards.nine.googleplay.processes.{ CardsProcesses, Wiring }
import cards.nine.googleplay.service.free.interpreter.TestData._
import cats.syntax.either._
import org.specs2.matcher.TaskMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll

class InterpreterIntegration extends Specification with AfterAll {

  import TaskMatchers._

  val process: CardsProcesses[GooglePlayApp] = CardsProcesses.processes[GooglePlayApp]

  implicit val actorSystem: ActorSystem = ActorSystem("cards-nine-googleplay-tests")
  import scala.concurrent.ExecutionContext.Implicits.global

  val interpreter: Wiring = new Wiring(nineCardsConfiguration)

  override def afterAll: Unit = interpreter.shutdown()

  sequential

  "The ApiClient, the client to Google Play unofficial API" should {

    "Making an API request for a Card" should {

      "result in an Item for packages that exist" in {
        def eraseDetails(card: FullCard): FullCard = card.copy(
          downloads   = "",
          categories  = card.categories.take(1),
          screenshots = List(),
          stars       = 3.145
        )
        val response = process
          .getCard(fisherPrice.packageObj, marketAuth)
          .foldMap(interpreter)
          .map(_.leftMap(_ ⇒ InfoError(fisherPrice.packageName)))

        val fields = response.map(_.map(eraseDetails))
        // The number of downloads can be different from the Google API.
        fields must returnValue(Right(eraseDetails(fisherPrice.card)))
      }

      "result in an error state for packages that do not exist" in {
        val response = process
          .getCard(nonexisting.packageObj, marketAuth)
          .foldMap(interpreter)
          .map(_.leftMap(_ ⇒ InfoError(nonexisting.packageName)))

        response must returnValue(Left(nonexisting.infoError))
      }
    }

  }

}