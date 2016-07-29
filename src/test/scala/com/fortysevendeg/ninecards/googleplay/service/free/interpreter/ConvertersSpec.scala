package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain._
import java.io.FileInputStream
import java.nio.file.{ Files, Paths}
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.specs2.mutable.Specification
import org.xml.sax.InputSource
import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter
import scodec.bits.ByteVector
import spray.testkit.Specs2RouteTest

class ConvertersSpec extends Specification with Specs2RouteTest {

  import TestData._

  def readProtobufFile(fileName: String): ByteVector = {
    val resource = getClass.getClassLoader.getResource(fileName)
    resource != null aka s"Test protobuf response file [$fileName] must exist" must beTrue
    ByteVector.apply( Files.readAllBytes(Paths.get(resource.getFile)) )
  }

  def readHtmlFile(fileName:String): Node = {
    val resource = getClass.getClassLoader.getResource(fileName + ".html")
    val parser = new SAXFactoryImpl().newSAXParser()
    val adapter = new NoBindingFactoryAdapter
    adapter.loadXML(new InputSource(new FileInputStream(resource.getFile)), parser)
  }

  "Parsing the binary response" should {

    "result in an Item to send to the client" in {
      val byteVector = readProtobufFile(fisherPrice.packageName)
      val item: Item = GoogleApiItemParser.parseItem(byteVector)
      item.docV2.docid must_=== fisherPrice.packageName
    }

    "result in a Card to send to the client" in {
      val byteVector = readProtobufFile(fisherPrice.packageName)
      val card: AppCard = GoogleApiItemParser.parseCard(byteVector)
      card.packageName must_=== fisherPrice.packageName
      card.free must_=== fisherPrice.card.free
      card.icon must_=== fisherPrice.card.icon
    }

    "correctly interpret if the app is free (zero price) or not" in {
      val byteVector = readProtobufFile(minecraft.packageName)
      val card: AppCard = GoogleApiItemParser.parseCard(byteVector)
      card.free must_=== minecraft.card.free
      card.icon must_=== minecraft.card.icon
    }

  }

  "Parsing the HTML for the play store" should {
    "result in an Item to send to the client" in {
      val parsedItem = GooglePlayPageParser
        .parseItemAux(readHtmlFile(fisherPrice.packageName))
        .getOrElse(failTest("Item should parse correctly"))

      parsedItem.docV2.details.appDetails.appCategory must_=== fisherPrice.card.categories
      parsedItem.docV2.docid must_=== fisherPrice.packageName
      parsedItem.docV2.title must_=== fisherPrice.card.title
    }

    "result in an AppCard to send to the client" in {
      val card = GooglePlayPageParser
        .parseCardAux(readHtmlFile(fisherPrice.packageName))
        .getOrElse(failTest("AppCard should parse correctly"))

      card.categories must_=== fisherPrice.card.categories
      card.packageName must_=== fisherPrice.packageName
      card.title must_=== fisherPrice.card.title
      card.free must_=== fisherPrice.card.free
      card.icon must_=== fisherPrice.card.icon
      card.stars must_=== 4.069400310516357
      card.downloads must_=== "1.000.000 - 5.000.000"
      card.categories must_=== List("EDUCATION", "FAMILY_EDUCATION")
    }

    "correctly interpret if the app is free (zero price) or not" in {
      val card = GooglePlayPageParser
        .parseCardAux(readHtmlFile(minecraft.packageName))
        .getOrElse(failTest("AppCard should parse correctly"))

      card.free must beEqualTo( minecraft.card.free)
      card.icon must beEqualTo( minecraft.card.icon)
    }

  }

}
