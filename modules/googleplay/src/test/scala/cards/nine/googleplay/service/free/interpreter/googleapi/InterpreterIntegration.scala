package cards.nine.googleplay.service.free.interpreter.googleapi

import cats.data.Xor
import cards.nine.domain.application.FullCard
import cards.nine.googleplay.domain._
import cards.nine.googleplay.processes.Wiring
import cards.nine.googleplay.config.TestConfig._
import cards.nine.googleplay.service.free.interpreter.TestData._
import org.specs2.matcher.TaskMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll

class InterpreterIntegration extends Specification with AfterAll {

  import TaskMatchers._

  private val appCardService = Wiring.appCardService

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
        val appRequest = AppRequest(fisherPrice.packageObj, marketAuth)
        val response = appCardService(appRequest)
        val fields = response.map(_.map(eraseDetails))
        // The number of downloads can be different from the Google API.
        fields must returnValue(Xor.Right(eraseDetails(fisherPrice.card)))
      }

      "result in an error state for packages that do not exist" in {
        val appRequest = AppRequest(nonexisting.packageObj, marketAuth)
        appCardService(appRequest) must returnValue(Xor.left(nonexisting.infoError))
      }
    }

  }

}