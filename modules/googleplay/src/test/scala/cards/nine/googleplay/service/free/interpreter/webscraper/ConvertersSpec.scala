package cards.nine.googleplay.service.free.interpreter.webscrapper

import cards.nine.googleplay.service.free.interpreter.TestData._
import java.io.FileInputStream
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.specs2.mutable.Specification
import org.xml.sax.InputSource
import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter

class ConvertersSpec extends Specification {

  import TestData._

  def readHtmlFile(fileName: String): Node = {
    val resource = getClass.getClassLoader.getResource(fileName + ".html")
    val parser = new SAXFactoryImpl().newSAXParser()
    val adapter = new NoBindingFactoryAdapter
    adapter.loadXML(new InputSource(new FileInputStream(resource.getFile)), parser)
  }

  def failTest(s: String) = throw new RuntimeException(s)

  "Parsing the HTML for the play store" should {
    "result in an Item to send to the client" in {
      val parsedItem = GooglePlayPageParser
        .parseItemAux(readHtmlFile(fisherPrice.packageName))
        .getOrElse(failTest("Item should parse correctly"))

      parsedItem.docV2.details.appDetails.appCategory must_=== fisherPrice.card.categories
      parsedItem.docV2.docid must_=== fisherPrice.packageName
      parsedItem.docV2.title must_=== fisherPrice.card.title
    }

    "result in an FullCard to send to the client" in {
      val card = GooglePlayPageParser
        .parseCardAux(readHtmlFile(fisherPrice.packageName))
        .getOrElse(failTest("FullCard should parse correctly"))

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
        .getOrElse(failTest("FullCard should parse correctly"))

      card.free must beEqualTo(minecraft.card.free)
      card.icon must beEqualTo(minecraft.card.icon)
    }

  }

}
