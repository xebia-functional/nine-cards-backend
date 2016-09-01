package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.data.Xor
import com.fortysevendeg.extracats.XorTaskOrComposer
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay.{Resolve, ResolveMany, RecommendationsByCategory}
import com.fortysevendeg.ninecards.googleplay.TestConfig._
import com.fortysevendeg.ninecards.googleplay.util.WithHttp1Client
import org.specs2.matcher.TaskMatchers
import org.specs2.mutable.Specification
import scala.concurrent.duration._
import scalaz.concurrent.Task
import TestData._

class InterpretersIntegration extends Specification with WithHttp1Client {

  import TaskMatchers._

  private val apiService = new googleapi.ApiServices(
    new googleapi.ApiClient( googleApiConf, pooledClient))
  private val webClient = new Http4sGooglePlayWebScraper(webEndpoint, pooledClient)

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

    "result in an AppCard for packages that exist" in {
      val appRequest = AppRequest(fisherPrice.packageObj, auth)
      val response: Task[Xor[InfoError, AppCard]] = webClient.getCard(appRequest)
      val relevantDetails = response.map { xor => xor.map { c: AppCard =>
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

class TaskInterpreterIntegration extends Specification with TaskMatchers with WithHttp1Client {

  sequential

  private val apiService = new googleapi.ApiServices(
    new googleapi.ApiClient( googleApiConf, pooledClient))
  val webClient = new Http4sGooglePlayWebScraper(webEndpoint, pooledClient)

  // Most of this should be moved to a wiring module, with the cache.
  val interpreter = {
    val itemService = new XorTaskOrComposer[AppRequest,String,Item](apiService.getItem, webClient.getItem)
    val cardService = new XorTaskOrComposer[AppRequest,InfoError, AppCard](apiService.getCard, webClient.getCard)
    new TaskInterpreter(itemService, cardService, apiService.recommendationsByCategory)
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

      val invalidPackages = List(nonexisting.packageName, "com.another.invalid.package")
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
        val appCardService: AppRequest => Task[Xor[InfoError, AppCard]] =
          (_ => Task.fail( new RuntimeException("Should not ask for App Card")))
        val recommend: RecommendationsByCategory => Task[Xor[InfoError,AppRecommendationList]] =
          ( _ => Task.fail( new RuntimeException("Should not ask for recommendations")))
        new TaskInterpreter( itemService, appCardService, recommend)
      }

      val retrievedCategory: Task[Option[String]] = interpreter
        .apply(Resolve(authParams, fisherPrice.packageObj))
        .map( _.flatMap(categoryOption))

      retrievedCategory.runFor(10.seconds) must_=== Some("EDUCATION")
    }
  }
}
