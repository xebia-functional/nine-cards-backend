package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.config.NineCardsConfig
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._
import org.specs2.mutable.Specification
import org.specs2.matcher.TaskMatchers
import spray.testkit.Specs2RouteTest
import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.xml.sax.InputSource
import java.io.FileInputStream
import scalaz.concurrent.Task
import cats.data.Xor

class Http4sGooglePlayWebScraperIntegrationTest extends Specification with Specs2RouteTest with TaskMatchers {

  val packageName = "air.fisherprice.com.shapesAndColors"

  val expectedCategories = List("EDUCATION", "FAMILY_EDUCATION")
  val expectedDocId = "air.fisherprice.com.shapesAndColors"
  val expectedTitle = "Shapes & Colors Music Show"

  val webEndpoint = NineCardsConfig.getConfigValue("googleplay.web.endpoint")
  val webClient = new Http4sGooglePlayWebScraper(webEndpoint)

  "Parsing the HTML for the play store" should {
    "result in an Item to send to the client" in {

      val parser = new SAXFactoryImpl().newSAXParser()
      val adapter = new NoBindingFactoryAdapter

      val resource = getClass.getClassLoader.getResource(packageName + ".html")

      def doc: Node = adapter.loadXML(new InputSource(new FileInputStream(resource.getFile)), parser)

      val parsedItem = webClient.parseResponseToItem(doc).getOrElse(failTest("Item should parse correctly"))

      parsedItem.docV2.details.appDetails.appCategory must_=== expectedCategories
      parsedItem.docV2.docid must_=== expectedDocId
      parsedItem.docV2.title must_=== expectedTitle
    }
  }

  "Making a scrape request against the Play Store" should {
    "result in an Item for packages that exist" in {
      val request: Task[Xor[String, Item]] = webClient.request(Package(packageName), Some(Localization("es-ES")))
      val relevantDetails = request.map { xor =>
        xor.map { i: Item =>
          (i.docV2.docid, i.docV2.details.appDetails.appCategory, i.docV2.title)
        }
      }

      relevantDetails must returnValue(Xor.right((expectedDocId, expectedCategories, expectedTitle)))
    }
    "result in an error state for packages that do not exist" in {
      val unknownPackage = "com.package.does.not.exist"
      val request = webClient.request(Package(unknownPackage), Some(Localization("es-ES")))

      request must returnValue(Xor.left(unknownPackage))
    }
  }
}
