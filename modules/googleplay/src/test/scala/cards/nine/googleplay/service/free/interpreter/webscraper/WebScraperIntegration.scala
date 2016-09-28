package cards.nine.googleplay.service.free.interpreter.webscrapper

import cats.data.Xor
import cards.nine.googleplay.config.TestConfig._
import cards.nine.googleplay.domain._
import cards.nine.googleplay.service.free.interpreter.TestData._
import cards.nine.googleplay.util.WithHttp1Client
import org.specs2.matcher.TaskMatchers
import org.specs2.mutable.Specification
import scalaz.concurrent.Task

class InterpretersIntegration extends Specification with WithHttp1Client {

  import TaskMatchers._

  private val webClient = new Http4sGooglePlayWebScraper(webEndpoint, pooledClient)

  sequential

  "Http4sGooglePlayWebScraper, the parser of Google Play's pages" should {

    val auth = GoogleAuthParams(AndroidId(""), Token(""), Some(localization))

    "result in an Item for packages that exist" in {
      val appRequest = AppRequest(fisherPrice.packageObj, auth)
      val response: Task[Xor[String, Item]] = webClient.getItem(appRequest)
      val relevantDetails = response.map ( _.map { i: Item =>
        (i.docV2.docid, i.docV2.details.appDetails.appCategory, i.docV2.title)
      })
      val expected = (fisherPrice.packageName, fisherPrice.card.categories, fisherPrice.card.title)
      relevantDetails must returnValue(Xor.right(expected))
    }

    "result in an error state for packages that do not exist" in {
      val appRequest = AppRequest(nonexisting.packageObj, auth)
      val response = webClient.getItem(appRequest)
      response must returnValue(Xor.left(nonexisting.packageName))
    }

    "result in an FullCard for packages that exist" in {
      val appRequest = AppRequest(fisherPrice.packageObj, auth)
      val response: Task[Xor[InfoError, FullCard]] = webClient.getCard(appRequest)
      val relevantDetails = response.map { xor => xor.map { c: FullCard =>
        (c.packageName, c.categories, c.title)
      }}
      val expected = (fisherPrice.packageName, fisherPrice.card.categories, fisherPrice.card.title)
      relevantDetails must returnValue(Xor.right(expected))
    }

    "result in an error state for packages that do not exist" in {
      val appRequest = AppRequest(nonexisting.packageObj, auth)
      val response = webClient.getCard(appRequest)
      response must returnValue(Xor.left(nonexisting.infoError ))
    }

  }

}
