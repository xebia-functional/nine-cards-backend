package cards.nine.googleplay.service.free.interpreter.googleapi

import cards.nine.commons.catscalaz.TaskInstances._
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

  override def afterAll: Unit = Wiring.shutdown()

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
          .foldMap(Wiring.interpreters)
          .map(_.leftMap(_ ⇒ InfoError(fisherPrice.packageName)))

        val fields = response.map(_.map(eraseDetails))
        // The number of downloads can be different from the Google API.
        fields must returnValue(Right(eraseDetails(fisherPrice.card)))
      }

      "result in an error state for packages that do not exist" in {
        val response = process
          .getCard(nonexisting.packageObj, marketAuth)
          .foldMap(Wiring.interpreters)
          .map(_.leftMap(_ ⇒ InfoError(nonexisting.packageName)))

        response must returnValue(Left(nonexisting.infoError))
      }
    }

  }

}