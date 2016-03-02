package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import org.specs2.mutable.Specification
import org.specs2.matcher.TaskMatchers
import spray.testkit.Specs2RouteTest
import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.xml.sax.InputSource
import java.io.FileInputStream

class Http4sGooglePlayWebScraperIntegrationTest extends Specification with Specs2RouteTest with TaskMatchers {

  val packageName = "air.fisherprice.com.shapesAndColors"

  "Parsing the HTML for the play store" should {
    "result in an Item to send to the client" in {

      val parser = new SAXFactoryImpl().newSAXParser()
      val adapter = new NoBindingFactoryAdapter

      val resource = getClass.getClassLoader.getResource(packageName + ".html")

      val expectedPackages = List("EDUCATION", "FAMILY_EDUCATION")
      val expectedDocId = "air.fisherprice.com.shapesAndColors"
      val expectedTitle = "Shapes & Colors Music Show"
      
      def doc: Node = adapter.loadXML(new InputSource(new FileInputStream(resource.getFile)), parser)

      val parsedItem = Http4sGooglePlayWebScraper.parseResponseToItem(doc).getOrElse(failTest("Item should parse correctly"))

      parsedItem.docV2.docid must_=== packageName
      parsedItem.docV2.details.appDetails.appCategory must_=== expectedPackages
      parsedItem.docV2.docid must_=== expectedDocId
      parsedItem.docV2.title must_=== expectedTitle
    }
  }
}
