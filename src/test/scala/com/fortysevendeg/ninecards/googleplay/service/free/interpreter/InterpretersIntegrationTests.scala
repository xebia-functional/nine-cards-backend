package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.data.Xor
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import com.fortysevendeg.ninecards.googleplay.TestConfig._
import org.specs2.matcher.TaskMatchers
import org.specs2.mutable.Specification
import org.http4s.client.blaze.PooledHttp1Client
import scala.concurrent.duration._
import scalaz.concurrent.Task

class GooglePlayApiIntegrationTests extends Specification with TaskMatchers {

  import TestData._

  // Most of this should be moved to a wiring module, with the cache.
  private val client = PooledHttp1Client()
  private val apiClient = new Http4sGooglePlayApiClient(apiEndpoint, client)

  "Making an API request" should {

    "result in an Item for packages that exist" in {
      val appRequest = AppRequest(fisherPrice.packageObj, authParams )
      val fetchedDocId = apiClient(appRequest).map(xor => xor.map(_.docV2.docid))
      fetchedDocId must returnValue(Xor.right(fisherPrice))
      // todo should this be more comprehensive? check all other tests too
    }

    "result in an error state for packages that do not exist" in {
      val appRequest = AppRequest(Package(nonexisting), authParams )
      apiClient(appRequest) must returnValue(Xor.left(nonexisting))
    }
  }

}

class WebScrapperIntegrationTests extends Specification with TaskMatchers {

  import TestData._

  // Most of this should be moved to a wiring module, with the cache.
  private val client = PooledHttp1Client()
  private val webClient = new Http4sGooglePlayWebScraper(webEndpoint, client)

  "Making a Web scrape request against the Play Store" should {

    val auth = GoogleAuthParams(AndroidId(""), Token(""), Some(localization))

    "result in an Item for packages that exist" in {
      val request: Task[Xor[String, Item]] = webClient(AppRequest(fisherPrice.packageObj, auth))
      val relevantDetails = request.map { xor =>
        xor.map { i: Item =>
          (i.docV2.docid, i.docV2.details.appDetails.appCategory, i.docV2.title)
        }
      }
      relevantDetails must returnValue(Xor.right((fisherPrice.packageName, fisherPrice.categories, fisherPrice.title)))
    }

    "result in an error state for packages that do not exist" in {
      val request = webClient(AppRequest(Package(nonexisting), auth))
      request must returnValue(Xor.left(nonexisting))
    }
  }

}

class InterpretersIntegrationTests extends Specification with TaskMatchers {

  import TestData._

  // Most of this should be moved to a wiring module, with the cache.
  private val client = PooledHttp1Client()
  private val apiClient = new Http4sGooglePlayApiClient(apiEndpoint, client)
  private val webClient = new Http4sGooglePlayWebScraper(webEndpoint, client)
  private val interpreter = TaskInterpreter(apiClient, webClient)

  "Making requests to the Google Play store" should {

    "result in a correctly parsed response for a single package" in {

      val result = interpreter(RequestPackage(authParams, fisherPrice.packageObj))

      val retrievedCategory = result.map { optionalItem =>
        optionalItem.flatMap(_.docV2.details.appDetails.appCategory.headOption)
      }

      retrievedCategory must returnValue(Some("EDUCATION"))
    }

    "result in a correctly parsed response for multiple packages" in {

      val successfulCategories = List(
        (fisherPrice.packageName, "EDUCATION"),
        ("com.google.android.googlequicksearchbox", "TOOLS")
      )

      val invalidPackages = List(nonexisting, "com.another.invalid.package")

      val packages: List[String] = successfulCategories.map(_._1) ++ invalidPackages

      val response = interpreter(BulkRequestPackage(authParams, PackageList(packages)))

      val result = response.map { case PackageDetails(errors, items) =>
        val itemCategories = items.flatMap(_.docV2.details.appDetails.appCategory)

        (errors.sorted, itemCategories.sorted)
      }

      result must returnValue((invalidPackages.sorted, successfulCategories.map(_._2).sorted))
    }
  }

  "Making requests when the Google Play API is not successful" should {
    "fail over to the web scraping approach" in {

      val badApiRequest: AppService = ( _ => Task.fail(new RuntimeException("Failed request")) )

      val badApiClient = new Http4sGooglePlayApiClient("http://unknown.host.com", client)
      val interpreter = TaskInterpreter(badApiClient, webClient)

      val result = interpreter(RequestPackage(authParams, fisherPrice.packageObj))

      val retrievedCategory = result.map { optionalItem =>
        optionalItem.flatMap(_.docV2.details.appDetails.appCategory.headOption)
      }

      retrievedCategory.runFor(10.seconds) must_=== Some("EDUCATION")
    }
  }
}
