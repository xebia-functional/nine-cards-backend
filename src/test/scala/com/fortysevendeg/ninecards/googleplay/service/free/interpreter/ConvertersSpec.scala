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

  import TestData.fisherPrice

  def readProtobufFile(): ByteVector = {
    val resource = getClass.getClassLoader.getResource(fisherPrice.packageName)
    resource != null aka s"Test protobuf response file [${fisherPrice.packageName}] must exist" must beTrue
    ByteVector.apply( Files.readAllBytes(Paths.get(resource.getFile)) )
  }

  def readHtmlFile(): Node = {
    val resource = getClass.getClassLoader.getResource(fisherPrice.packageName + ".html")
    val parser = new SAXFactoryImpl().newSAXParser()
    val adapter = new NoBindingFactoryAdapter
    adapter.loadXML(new InputSource(new FileInputStream(resource.getFile)), parser)
  }

  "Parsing the binary response" should {

    "result in an Item to send to the client" in {
      val byteVector = readProtobufFile()
      val item: Item = GoogleApiItemParser.parseItem(byteVector)
      item.docV2.docid must_=== fisherPrice.packageName
    }

    "result in an Card to send to the client" in {
      val byteVector = readProtobufFile()
      val item: AppCard = GoogleApiItemParser.parseCard(byteVector)
      item.packageName must_=== fisherPrice.packageName
    }

  }

  "Parsing the HTML for the play store" should {
    "result in an Item to send to the client" in {
      val doc = readHtmlFile() 

      val parsedItem = GooglePlayPageParser
        .parseItemAux(doc).getOrElse(failTest("Item should parse correctly"))

      parsedItem.docV2.details.appDetails.appCategory must_=== fisherPrice.categories
      parsedItem.docV2.docid must_=== fisherPrice.packageName
      parsedItem.docV2.title must_=== fisherPrice.title
    }

    "result in an AppCard to send to the client" in {
      val doc = readHtmlFile() 

      val card = GooglePlayPageParser.parseCardAux(doc)
        .getOrElse(failTest("AppCard should parse correctly"))

      card.categories must_=== fisherPrice.categories
      card.packageName must_=== fisherPrice.packageName
      card.title must_=== fisherPrice.title
      card.free must_=== true
      card.icon must_=== "http://lh4.ggpht.com/Pb8iLNmi9vHOwB-39TKe-kn4b_uU-E6rn7zSiFz6jC0RlaEQeNCcBh2MueyslcQ3mj2H"
      card.stars must_=== 4.069400310516357
      card.downloads must_=== "1.000.000 - 5.000.000"
      card.categories must_=== List("EDUCATION", "FAMILY_EDUCATION")
    }

  }

}
