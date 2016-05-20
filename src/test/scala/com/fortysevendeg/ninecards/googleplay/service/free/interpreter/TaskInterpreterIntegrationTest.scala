package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.config.NineCardsConfig
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import com.fortysevendeg.ninecards.googleplay.TestConfig
import org.specs2.mutable.Specification
import org.specs2.matcher.TaskMatchers
import spray.testkit.Specs2RouteTest
import scalaz.concurrent.Task
import cats.data.Xor
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain.GoogleAuthParams
import scala.concurrent.duration._

class TaskInterpreterIntegrationTest extends Specification with Specs2RouteTest with TaskMatchers with TestConfig {

  val apiEndpoint = NineCardsConfig.getConfigValue("googleplay.api.endpoint")
  val apiClient = new Http4sGooglePlayApiClient(apiEndpoint)
  val webEndpoint = NineCardsConfig.getConfigValue("googleplay.web.endpoint")
  val webClient = new Http4sGooglePlayWebScraper(webEndpoint)

  val interpreter = TaskInterpreter.interpreter(apiClient.request _, webClient.request _)

  "Making requests to the Google Play store" should {
    "result in a correctly parsed response for a single package" in {

      val expectedCategory = "EDUCATION"

      val result = interpreter(RequestPackage(params, Package("air.fisherprice.com.shapesAndColors")))

      val retrievedCategory = result.map { optionalItem =>
        optionalItem.flatMap(_.docV2.details.appDetails.appCategory.headOption)
      }

      retrievedCategory must returnValue(Some(expectedCategory))
    }

    "result in a correctly parsed response for multiple packages" in {

      val successfulCategories = List(
        ("air.fisherprice.com.shapesAndColors", "EDUCATION"),
        ("com.google.android.googlequicksearchbox", "TOOLS")
      )

      val invalidPackages = List("com.package.does.not.exist", "com.another.invalid.package")

      val packages = successfulCategories.map(_._1) ++ invalidPackages

      val response = interpreter(BulkRequestPackage(params, PackageListRequest(packages)))

      val result = response.map { case PackageDetails(errors, items) =>
        val itemCategories = items.flatMap(_.docV2.details.appDetails.appCategory)

        (errors.sorted, itemCategories.sorted)
      }

      result must returnValue((invalidPackages.sorted, successfulCategories.map(_._2).sorted))
    }
  }

  "Making requests when the Google Play API is not successful" should {
    "fail over to the web scraping approach" in {

      val badApiRequest: (Package, GoogleAuthParams) => Task[Xor[String, Item]] = { (_, _) =>
        Task.fail(new RuntimeException("Failed request"))
      }


      val interpreter = TaskInterpreter.interpreter((new Http4sGooglePlayApiClient("http://unknown.host.com")).request _, webClient.request)

      val expectedCategory = "EDUCATION"

      val result = interpreter(RequestPackage(params, Package("air.fisherprice.com.shapesAndColors")))

      val retrievedCategory = result.map { optionalItem =>
        optionalItem.flatMap(_.docV2.details.appDetails.appCategory.headOption)
      }

      retrievedCategory.runFor(10.seconds) must_=== Some(expectedCategory)
    }
  }
}
