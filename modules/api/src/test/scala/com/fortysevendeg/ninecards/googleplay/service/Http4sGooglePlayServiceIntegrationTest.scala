package com.fortysevendeg.ninecards.googleplay.service

import org.specs2.mutable.Specification
import org.specs2.matcher.TaskMatchers
import spray.testkit.Specs2RouteTest
import GooglePlayService._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import java.nio.file.Paths
import java.nio.file.Files

class Http4sGooglePlayServiceIntegrationTest extends Specification with Specs2RouteTest with TaskMatchers {

  val packageName = "air.fisherprice.com.shapesAndColors"

  "Parsing the binary response" should {
    "result in an Item to send to the client" in {

      val resource = getClass.getClassLoader.getResource(packageName)
      resource != null aka s"Test protobuf response file [$packageName] must exist" must beTrue

      val bytes = Files.readAllBytes(Paths.get(resource.getFile))
      val byteVector = scodec.bits.ByteVector.apply(bytes)

      val item: Item = Http4sGooglePlayService.parseResponseToItem(byteVector)

      item.docV2.docid must_=== packageName
    }
  }

  val token = Token("DQAAABQBAACJr1nBqQRTmbhS7yFG8NbWqkSXJchcJ5t8FEH-FNNtpk0cU-Xy8-nc_z4fuQV3Sw-INSFK_NuQnafoqNI06nHPD4yaqXVnQbonrVsokBKQnmkQ9SsD0jVZi8bUsC4ehd-w2tmEe7SZ_8vXhw_3f1iNnsrAqkpEvbPkFIo9oZeAq26us2dTo22Ttn3idGoua8Is_PO9EKzItDQD-0T9QXIDDl5otNMG5T4MS9vrbPOEhjorHqGfQJjT8Y10SK2QdgwwyIF2nCGZ6N-E-hbLjD0caXkY7ATpzhOUIJNnBitIs-h52E8JzgHysbYBK9cy6k6Im0WPyHvzXvrwsUK2RTwh-YBpFVSpBACmc89OZKnYE-VfgKHg9SSv1aNrBeEETQE")
  val androidId = AndroidId("3D4D7FE45C813D3E")
  val localization = Localization("es-ES")

  val params = (token, androidId, Some(localization))

  "Making a request to the Google Play store" should {
    "result in a correctly parsed response" in {

      val expectedCategory = "EDUCATION"

      val result = Http4sGooglePlayService.packageRequest(params)(Package(packageName))

      val retrievedCategory = result.map { optionalItem =>
        optionalItem.flatMap(_.docV2.details.appDetails.appCategory.headOption)
      }

      retrievedCategory must returnValue(Some(expectedCategory))
    }
  }
}
