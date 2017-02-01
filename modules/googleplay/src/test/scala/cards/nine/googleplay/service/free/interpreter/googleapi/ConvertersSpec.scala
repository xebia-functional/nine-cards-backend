/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.googleplay.service.free.interpreter.googleapi

import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.googleplay.proto.GooglePlay.{ ResponseWrapper, DocV2, ListResponse, SearchResponse }
import cards.nine.googleplay.service.free.interpreter.TestData.{ fisherPrice, minecraft }
import java.nio.file.{ Files, Paths }
import org.specs2.mutable.Specification
import scodec.bits.ByteVector

class ConvertersSpec extends Specification {

  import Converters._
  import TestData.searchCosmos

  def readProtobufFile(fileName: String): ResponseWrapper = {
    val resource = getClass.getClassLoader.getResource(fileName)
    resource != null aka s"Test protobuf response file [$fileName] must exist" must beTrue
    val bv = ByteVector.apply(Files.readAllBytes(Paths.get(resource.getFile)))
    ResponseWrapper.parseFrom(bv.toArray)
  }

  def getDetailsResponse(rw: ResponseWrapper): DocV2 = rw.getPayload.getDetailsResponse.getDocV2
  def getListResponse(rw: ResponseWrapper): ListResponse = rw.getPayload.getListResponse

  "From a DocV2 carrying an application's details, it " should {

    "result in a Card to send to the client" in {
      val docV2: DocV2 = getDetailsResponse(readProtobufFile(fisherPrice.packageName))
      val card: FullCard = toFullCard(docV2)
      card.packageName must_=== fisherPrice.packageObj
      card.free must_=== fisherPrice.card.free
      card.icon must_=== fisherPrice.card.icon
    }

    "correctly interpret if the app is free (zero price) or not" in {
      val docV2: DocV2 = getDetailsResponse(readProtobufFile(minecraft.packageName))
      val card: FullCard = toFullCard(docV2)
      card.free must_=== minecraft.card.free
      card.icon must_=== minecraft.card.icon
    }

    "get a full card with some screenshots" in {
      val docV2: DocV2 = getDetailsResponse(readProtobufFile(fisherPrice.packageName))
      val rec: FullCard = toFullCard(docV2)
      rec.packageName must_=== fisherPrice.packageObj
      rec.screenshots.length must beGreaterThan(0)
    }
  }

  "From a ListResponse carrying the result of a category recommendations, it" should {
    val fileName = "recommend/SOCIAL_FREE"
    val listRes: ListResponse = getListResponse(readProtobufFile(fileName))

    "read the list of ids of the recommended apps" in {
      val ids: List[Package] = listResponseToPackages(listRes)
      val expected = List(
        "com.facebook.katana",
        "com.instagram.android",
        "com.pinterest",
        "com.pof.android",
        "com.linkedin.android",
        "com.tumblr",
        "com.sgiggle.production",
        "com.zoosk.zoosk",
        "co.vine.android",
        "com.timehop",
        "jp.naver.lineplay.android",
        "com.okcupid.okcupid",
        "com.myyearbook.m",
        "com.jaumo"
      ) map Package

      ids must containTheSameElementsAs(expected)
    }
  }

  "From a series of lists responses, each one carrying the recommendations for a different app, it" should {

    val files = List("com.facebook.katana", "com.instagram.android", "com.pinterest")
    def listResponseOf(file: String) = getListResponse(readProtobufFile(s"recommend/$file"))
    def packagesOf(file: String) = listResponseToPackages(listResponseOf(file)).map(_.value)

    "read the list of ids of the recommended apps" in {

      packagesOf(files(0)) must containTheSameElementsAs(
        List("com.instagram.android", "com.facebook.orca", "com.whatsapp")
      )
      packagesOf(files(1)) must containTheSameElementsAs(
        List("com.facebook.katana", "com.whatsapp", "com.facebook.orca")
      )
      packagesOf(files(2)) must containTheSameElementsAs(
        List("com.tumblr", "com.weheartit", "com.instagram.android")
      )
    }

    "extract a union list of recommended apps without repetitions" in {
      val ids: List[Package] = listResponseListToPackages(files map listResponseOf)
      ids.map(_.value) must containTheSameElementsAs(
        List("com.tumblr", "com.weheartit", "com.whatsapp",
          "com.instagram.android", "com.facebook.katana", "com.facebook.orca")
      )
    }
  }

  """From a SearchResponse, with the results of a search for words, """ should {

    def readSearchFile(file: String): SearchResponse =
      readProtobufFile(s"search/$file").getPayload.getSearchResponse

    "extract a list of search results for that App" in {
      val actual = searchResponseToPackages(readSearchFile("cosmos"))
      actual must containTheSameElementsAs(searchCosmos.results)
    }

  }

}
