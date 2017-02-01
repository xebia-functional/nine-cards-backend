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
package cards.nine.googleplay.service.free.interpreter.webscrapper

import cards.nine.domain.application.{ FullCard, Package }
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.xml.sax.InputSource
import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter
import scodec.bits.ByteVector

object GooglePlayPageParser {

  private val parser = new SAXFactoryImpl().newSAXParser()
  private val adapter = new NoBindingFactoryAdapter

  private def decodeNode(byteVector: ByteVector): Node =
    byteVector.decodeUtf8.fold(
      e ⇒ throw e,
      s ⇒ adapter.loadXML(new InputSource(new java.io.ByteArrayInputStream(s.getBytes)), parser)
    )

  implicit class NodeOps(node: Node) {

    def getAttribute(key: String): String = (node \ s"@$key").text

    def isProperty(key: String): Boolean = getAttribute("itemprop") == key

    def isClass(key: String): Boolean = getAttribute("class") == key
  }

  class NodeWrapper(doc: Node) {

    def getTitle(): Seq[String] =
      for /*Seq*/ {
        n ← doc \\ "div"
        if n.isClass("id-app-title")
      } yield n.text.trim

    def getDownloads(): Seq[String] =
      for /*Seq*/ {
        n ← doc \\ "div"
        if n.isProperty("numDownloads")
        if n.isClass("content")
      } yield n.text.trim

    def getStars(): Seq[Double] =
      for /*Seq*/ {
        n ← doc \\ "meta"
        if n.isProperty("ratingValue")
        stars = n.getAttribute("content")
      } yield stars.toDouble

    def getDocId(): Seq[String] =
      (doc \\ "@data-load-more-docid").map(_.text)

    /*We exclude from the URL the portions after the equals symbol, which are parameters
     to choose smaller icons */
    val ImageUrlRegex = "[^/]*//([^=]+)=??.*".r

    def getImages(kind: String): Seq[String] =
      for /*Seq*/ {
        n ← doc \\ "img"
        if n.isProperty(kind)
        url ← n.getAttribute("src") match {
          case ImageUrlRegex(uri) ⇒ Seq(uri)
          case _ ⇒ Seq()
        }
      } yield s"http://$url"

    def getIcon(): Seq[String] = getImages("image")
    def getScreenshots(): Seq[String] = getImages("screenshot")

    val CategoryHrefRegex = "/store/apps/category/(\\w+)".r

    def getCategories(): Seq[String] =
      for /*Seq*/ {
        n ← doc \\ "a"
        if n.isClass("document-subtitle category")
        cat ← n.getAttribute("href") match {
          case CategoryHrefRegex(cat) ⇒ Seq(cat)
          case _ ⇒ Seq()
        }
      } yield cat

    def isFree(): Seq[Boolean] =
      for /*Seq*/ {
        n ← doc \\ "meta"
        if n.isProperty("price")
        price = n.getAttribute("content").trim
      } yield price == "0"

    def parseCardAux(): Seq[FullCard] =
      for { /*Seq*/
        docId ← getDocId()
        title ← getTitle()
        free ← isFree()
        icon ← getIcon()
        stars ← getStars()
        downloads ← getDownloads()
      } yield FullCard(
        packageName = Package(docId),
        title       = title,
        free        = free,
        icon        = icon,
        stars       = stars,
        downloads   = downloads,
        screenshots = getScreenshots().toList,
        categories  = getCategories().toList
      )

    def parseCard(): Option[FullCard] = parseCardAux.headOption

  }

  def parseCard(byteVector: ByteVector): Option[FullCard] =
    parseCardAux(decodeNode(byteVector))

  def parseCardAux(document: Node): Option[FullCard] =
    new NodeWrapper(document).parseCard()

}

