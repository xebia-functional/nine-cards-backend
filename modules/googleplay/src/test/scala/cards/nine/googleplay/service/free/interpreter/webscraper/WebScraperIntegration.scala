package cards.nine.googleplay.service.free.interpreter.webscrapper

import cards.nine.domain.account.AndroidId
import cards.nine.domain.application.FullCard
import cards.nine.domain.market.{ MarketCredentials, MarketToken }
import cards.nine.googleplay.config.TestConfig._
import cards.nine.googleplay.domain._
import cards.nine.googleplay.service.free.interpreter.TestData._
import cards.nine.googleplay.util.WithHttp1Client
import cats.syntax.either._
import org.specs2.matcher.TaskMatchers
import org.specs2.mutable.Specification
import scalaz.concurrent.Task

class InterpretersIntegration extends Specification with WithHttp1Client {

  import TaskMatchers._

  private val webClient = new Http4sGooglePlayWebScraper(webEndpoint, pooledClient)

  sequential

  "Http4sGooglePlayWebScraper, the parser of Google Play's pages" should {

    val auth = MarketCredentials(AndroidId(""), MarketToken(""), Some(localization))

    "result in an FullCard for packages that exist" in {
      val appRequest = AppRequest(fisherPrice.packageObj, auth)
      val response: Task[InfoError Either FullCard] = webClient.getCard(appRequest)
      val relevantDetails = response.map { xor ⇒
        xor.map { c: FullCard ⇒
          (c.packageName, c.categories, c.title)
        }
      }
      val expected = (fisherPrice.packageObj, fisherPrice.card.categories, fisherPrice.card.title)
      relevantDetails must returnValue(Right(expected))
    }

    "result in an error state for packages that do not exist" in {
      val appRequest = AppRequest(nonexisting.packageObj, auth)
      val response = webClient.getCard(appRequest)
      response must returnValue(Left(nonexisting.infoError))
    }

  }

}
