package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi

import com.fortysevendeg.ninecards.googleplay.domain.{AppCard, AppRecommendation, AppRecommendationList, Item}
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.TestData.{ fisherPrice, minecraft }
import java.nio.file.{ Files, Paths}
import org.specs2.mutable.Specification
import scodec.bits.ByteVector
import spray.testkit.Specs2RouteTest

class ConvertersSpec extends Specification with Specs2RouteTest {

  import Converters._
  import proto.GooglePlay.{ResponseWrapper, DocV2, ListResponse}

  def readProtobufFile(fileName: String): ResponseWrapper = {
    val resource = getClass.getClassLoader.getResource(fileName)
    resource != null aka s"Test protobuf response file [$fileName] must exist" must beTrue
    val bv = ByteVector.apply( Files.readAllBytes(Paths.get(resource.getFile)) )
    ResponseWrapper.parseFrom(bv.toArray)
  }

  def getDetailsResponse(rw: ResponseWrapper): DocV2 = rw.getPayload.getDetailsResponse.getDocV2
  def getListResponse(rw: ResponseWrapper) : ListResponse = rw.getPayload.getListResponse

  "From a DocV2 carrying an application's details, it " should {

    "result in an Item to send to the client" in {
      val docV2: DocV2 = getDetailsResponse( readProtobufFile(fisherPrice.packageName) )
      val item: Item = toItem(docV2)
      item.docV2.docid must_=== fisherPrice.packageName
    }

    "result in a Card to send to the client" in {
      val docV2: DocV2 = getDetailsResponse( readProtobufFile(fisherPrice.packageName) )
      val card: AppCard = toCard(docV2)
      card.packageName must_=== fisherPrice.packageName
      card.free must_=== fisherPrice.card.free
      card.icon must_=== fisherPrice.card.icon
    }

    "correctly interpret if the app is free (zero price) or not" in {
      val docV2: DocV2 = getDetailsResponse( readProtobufFile(minecraft.packageName) )
      val card: AppCard = toCard(docV2)
      card.free must_=== minecraft.card.free
      card.icon must_=== minecraft.card.icon
    }

    "get an App recommendation with some screenshots" in {
      val docV2: DocV2 = getDetailsResponse( readProtobufFile(fisherPrice.packageName) )
      val rec: AppRecommendation = toAppRecommendation(docV2)
      rec.packageName must_=== fisherPrice.packageName
      rec.screenshots.length must beGreaterThan(0)
    }
  }

  "From a ListResponse carrying the result of a category recommendations, it" should {
    val fileName = "recommend_SOCIAL_FREE"
    val listRes: ListResponse = getListResponse( readProtobufFile(fileName))

    "read the list of ids of the recommended apps" in {
      val ids: List[String] = listResponseToPackages(listRes)
      ids must_=== List(
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
      )
    }

    "build incomplete cards of it" in {
      val apps: AppRecommendationList = toAppRecommendationList(listRes)
      apps.apps.map(_.packageName) must_=== listResponseToPackages(listRes)
    }

  }

}
