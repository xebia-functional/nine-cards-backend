package com.fortysevendeg.ninecards.googleplay.service

import GooglePlayDomain._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import org.specs2.mutable.Specification
import org.specs2.matcher.TaskMatchers
import spray.testkit.Specs2RouteTest
import java.nio.file.Paths
import java.nio.file.Files
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.Http4sTaskInterpreter
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._

class Http4sTaskInterpreterIntegrationTest extends Specification with Specs2RouteTest with TaskMatchers {

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

  val token = Token("DQAAABQBAACJr1nBqQRTmbhS7yFG8NbWqkSXJchcJ5t8FEH-FNNtpk0cU-Xy8-nc_z4fuQV3Sw-INSFK_NuQnafoqNI06nHPD4yaqXVnQbonrVsokBKQnmkQ9SsD0jVZi8bUsC4ehd-w2tmEe7SZ_8vXhw_3f1iNnsrAqkpEvbPkFIo9oZeAq26us2dTo22Ttn3idGoua8Is_PO9EKzItDQD-0T9QXIDDl5otNMG5T4MS9vrbPOEhjorHqGfQJjT8Y10SK2QdgwwyIF2nCGZ6N-E-hbLjD0caXkY7ATpzhOUIJNnBitIs-h52E8JzgHysbYBK9cy6k6Im0WPyHvzXvrwsUK2RTwh-YBpFVSpBACmc89OZKnYE-VfgKHg9SSv1aNrBeEETQE")
  val androidId = AndroidId("3D4D7FE45C813D3E")
  val localization = Localization("es-ES")

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
