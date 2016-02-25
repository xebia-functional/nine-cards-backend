package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import com.fortysevendeg.ninecards.googleplay.TestConfig
import org.specs2.mutable.Specification
import org.specs2.matcher.TaskMatchers
import spray.testkit.Specs2RouteTest
import java.nio.file.Paths
import java.nio.file.Files

class Http4sTaskInterpreterIntegrationTest extends Specification with Specs2RouteTest with TaskMatchers with TestConfig {

  val packageName = "air.fisherprice.com.shapesAndColors"

  "Parsing the binary response" should {
    "result in an Item to send to the client" in {

      val resource = getClass.getClassLoader.getResource(packageName)
      resource != null aka s"Test protobuf response file [$packageName] must exist" must beTrue

      val bytes = Files.readAllBytes(Paths.get(resource.getFile))
      val byteVector = scodec.bits.ByteVector.apply(bytes)

      val item: Item = Http4sTaskInterpreter.parseResponseToItem(byteVector)

      item.docV2.docid must_=== packageName
    }
  }

  val params = (token, androidId, Some(localization))

  "Making requests to the Google Play store" should {
    "result in a correctly parsed response for a single package" in {

      val expectedCategory = "EDUCATION"

      val result = Http4sTaskInterpreter.interpreter(RequestPackage(params, Package(packageName)))

      val retrievedCategory = result.map { optionalItem =>
        optionalItem.flatMap(_.docV2.details.appDetails.appCategory.headOption)
      }

      retrievedCategory must returnValue(Some(expectedCategory))
    }

    "result in a correctly parsed response for multiple packages" in {

      val successfulCategories = List(
        (packageName, "EDUCATION"),
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
