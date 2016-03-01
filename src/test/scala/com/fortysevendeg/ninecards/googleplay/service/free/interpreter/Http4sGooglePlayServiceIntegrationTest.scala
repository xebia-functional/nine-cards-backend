package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import com.fortysevendeg.ninecards.googleplay.TestConfig
import org.specs2.mutable.Specification
import org.specs2.matcher.TaskMatchers
import spray.testkit.Specs2RouteTest

class Http4sTaskInterpreterIntegrationTest extends Specification with Specs2RouteTest with TaskMatchers with TestConfig {

  val params = (token, androidId, Some(localization))

  "Making requests to the Google Play store" should {
    "result in a correctly parsed response for a single package" in {

      val expectedCategory = "EDUCATION"

      val result = Http4sTaskInterpreter.interpreter(RequestPackage(params, Package("air.fisherprice.com.shapesAndColors")))

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

      val response = Http4sTaskInterpreter.interpreter(BulkRequestPackage(params, PackageListRequest(packages)))

      val result = response.map { case PackageDetails(errors, items) =>
        val itemCategories = items.flatMap(_.docV2.details.appDetails.appCategory)

        (errors.sorted, itemCategories.sorted)
      }

      result must returnValue((invalidPackages.sorted, successfulCategories.map(_._2).sorted))
    }
  }
}
