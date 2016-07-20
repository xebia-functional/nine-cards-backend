package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.data.Xor
import com.fortysevendeg.extracats.XorTaskOrComposer
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay.{Resolve, ResolveMany}
import com.fortysevendeg.ninecards.googleplay.TestConfig._
import org.specs2.matcher.TaskMatchers
import org.specs2.mutable.Specification
import org.http4s.client.blaze.PooledHttp1Client
import scala.concurrent.duration._
import scalaz.concurrent.Task

class GooglePlayApiIntegration extends Specification with TaskMatchers {

  import TestData._

  // Most of this should be moved to a wiring module, with the cache.
  private val client = PooledHttp1Client()
  private val apiClient = new Http4sGooglePlayApiClient(apiEndpoint, client)

  "Making an API request" should {

    "result in an Item for packages that exist" in {
      val appRequest = AppRequest(fisherPrice.packageObj, authParams )
      val fetchedDocId = apiClient.getItem(appRequest).map(xor => xor.map(_.docV2.docid))
      fetchedDocId must returnValue(Xor.right(fisherPrice))
      // todo should this be more comprehensive? check all other tests too
    }

    "result in an error state for packages that do not exist" in {
      val appRequest = AppRequest(Package(nonexisting), authParams )
      apiClient.getItem(appRequest) must returnValue(Xor.left(nonexisting))
    }
  }

}

class WebScraperIntegration extends Specification with TaskMatchers {

  import TestData._

  // Most of this should be moved to a wiring module, with the cache.
  private val client = PooledHttp1Client()
  private val webClient = new Http4sGooglePlayWebScraper(webEndpoint, client)

  "Making a Web scrape request against the Play Store" should {

    val auth = GoogleAuthParams(AndroidId(""), Token(""), Some(localization))

    "result in an Item for packages that exist" in {
      val request: Task[Xor[String, Item]] = webClient.getItem(AppRequest(fisherPrice.packageObj, auth))
      val relevantDetails = request.map { xor =>
        xor.map { i: Item =>
          (i.docV2.docid, i.docV2.details.appDetails.appCategory, i.docV2.title)
        }
      }
      val expected = (fisherPrice.packageName, fisherPrice.categories, fisherPrice.title)
      relevantDetails must returnValue(Xor.right(expected))
    }

    "result in an error state for packages that do not exist" in {
      val request = webClient.getItem(AppRequest(Package(nonexisting), auth))
      request must returnValue(Xor.left(nonexisting))
    }
  }

}

class TaskInterpreterIntegration extends Specification with TaskMatchers {

  import TestData._

  val httpClient = PooledHttp1Client()
  val apiClient = new Http4sGooglePlayApiClient(apiEndpoint, httpClient)
  val webClient = new Http4sGooglePlayWebScraper(webEndpoint, httpClient)

  // Most of this should be moved to a wiring module, with the cache.
  val interpreter = {
    val itemService = new XorTaskOrComposer[AppRequest,String,Item](apiClient.getItem, webClient.getItem)
    val cardService = new XorTaskOrComposer[AppRequest,AppMissed, AppCard](apiClient.getCard, webClient.getCard)
    new TaskInterpreter(itemService, cardService)
  }

  def categoryOption(item: Item): Option[String] =
    item.docV2.details.appDetails.appCategory.headOption

  "Making requests to the Google Play store" should {

    def splitResults(res: PackageDetails) : (List[String],List[String]) = (
      res.errors.sorted,
      res.items.flatMap(categoryOption).sorted
    )

    "result in a correctly parsed response for a single package" in {
      val retrievedCategory: Task[Option[String]] = interpreter
        .apply( Resolve(authParams, fisherPrice.packageObj) )
        .map( optItem => optItem.flatMap(categoryOption) )
      retrievedCategory must returnValue(Some("EDUCATION"))
    }

    "result in a correctly parsed response for multiple packages" in {
      val successfulCategories = List(
        (fisherPrice.packageName, "EDUCATION"),
        ("com.google.android.googlequicksearchbox", "TOOLS")
      )

      val invalidPackages = List(nonexisting, "com.another.invalid.package")
      val packages: List[String] = successfulCategories.map(_._1) ++ invalidPackages

      val result = interpreter
        .apply( ResolveMany(authParams, PackageList(packages)) )
        .map(splitResults)
      result must returnValue((invalidPackages.sorted, successfulCategories.map(_._2).sorted))
    }
  }

  "Making requests when the Google Play API is not successful" should {
    "fail over to the web scraping approach" in {

      val interpreter = {
        val badApiRequest: AppRequest => Task[Xor[String,Item]] =
          ( _ => Task.fail(new RuntimeException("Failed request")) )
        val itemService: AppRequest => Task[Xor[String,Item]] =
          new XorTaskOrComposer( badApiRequest, webClient.getItem)
        val appCardService: AppRequest => Task[Xor[AppMissed, AppCard]] =
          (_ => Task.fail( new RuntimeException("Should not ask for App Card")))
        new TaskInterpreter( itemService, appCardService)
      }

      val retrievedCategory: Task[Option[String]] = interpreter
        .apply(Resolve(authParams, fisherPrice.packageObj))
        .map( _.flatMap(categoryOption))

      retrievedCategory.runFor(10.seconds) must_=== Some("EDUCATION")
    }
  }
}
