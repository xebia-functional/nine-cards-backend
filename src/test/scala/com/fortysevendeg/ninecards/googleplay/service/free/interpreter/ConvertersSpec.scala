package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.config.NineCardsConfig
import com.fortysevendeg.ninecards.googleplay.domain._
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.specs2.mutable.Specification
import org.xml.sax.InputSource
import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter
import spray.testkit.Specs2RouteTest

class ConvertersSpec extends Specification with Specs2RouteTest {

  import TestData.fisherPrice

  "Parsing the binary response" should {
    "result in an Item to send to the client" in {

      val resource = getClass.getClassLoader.getResource(fisherPrice.packageName)
      resource != null aka s"Test protobuf response file [${fisherPrice.packageName}] must exist" must beTrue

      val bytes = Files.readAllBytes(Paths.get(resource.getFile))
      val byteVector = scodec.bits.ByteVector.apply(bytes)

      val item: Item = ItemParser(byteVector)

      item.docV2.docid must_=== fisherPrice.packageName
    }
  }

  "Parsing the HTML for the play store" should {
    "result in an Item to send to the client" in {

      val parser = new SAXFactoryImpl().newSAXParser()
      val adapter = new NoBindingFactoryAdapter

      val resource = getClass.getClassLoader.getResource(fisherPrice.packageName + ".html")

      def doc: Node = adapter.loadXML(new InputSource(new FileInputStream(resource.getFile)), parser)

      val parsedItem = ByteVectorToItemParser
        .parseItem(doc).getOrElse(failTest("Item should parse correctly"))

      parsedItem.docV2.details.appDetails.appCategory must_=== fisherPrice.categories
      parsedItem.docV2.docid must_=== fisherPrice.packageName
      parsedItem.docV2.title must_=== fisherPrice.title
    }
  }

}
