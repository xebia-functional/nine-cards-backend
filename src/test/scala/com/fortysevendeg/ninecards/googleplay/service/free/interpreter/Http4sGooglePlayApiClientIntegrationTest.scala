package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import com.fortysevendeg.ninecards.googleplay.TestConfig
import org.specs2.mutable.Specification
import org.specs2.matcher.TaskMatchers
import spray.testkit.Specs2RouteTest
import java.nio.file.Paths
import java.nio.file.Files
import scalaz.concurrent.Task
import cats.data.Xor

class Http4sGooglePlayApiClientIntegrationTest extends Specification with Specs2RouteTest with TaskMatchers with TestConfig {

  val packageName = "air.fisherprice.com.shapesAndColors"

  "Parsing the binary response" should {
    "result in an Item to send to the client" in {

      val resource = getClass.getClassLoader.getResource(packageName)
      resource != null aka s"Test protobuf response file [$packageName] must exist" must beTrue

      val bytes = Files.readAllBytes(Paths.get(resource.getFile))
      val byteVector = scodec.bits.ByteVector.apply(bytes)

      val item: Item = Http4sGooglePlayApiClient.parseResponseToItem(byteVector)

      item.docV2.docid must_=== packageName
    }
  }

  "Making an API request" should {
    val headers = Http4sGooglePlayApiClient.headers(token, androidId, Some(localization))
    "result in an Item for packages that exist" in {
      val request = Http4sGooglePlayApiClient.request(Package(packageName), headers)
      val fetchedDocId = request.map(xor => xor.map(item => item.docV2.docid))

      fetchedDocId must returnValue(Xor.right(packageName))
    }
    "result in an error state for packages that do not exist" in {
      val unknownPackage = "com.package.does.not.exist"
      val request = Http4sGooglePlayApiClient.request(Package(unknownPackage), headers)

      request must returnValue(Xor.left(unknownPackage))
    }
  }
}
