package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain._
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.xml.sax.InputSource
import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter
import scodec.bits.ByteVector

object GooglePlayPageParser {

  private val parser = new SAXFactoryImpl().newSAXParser()
  private val adapter = new NoBindingFactoryAdapter

  private def decodeNode( byteVector: ByteVector): Node =
    byteVector.decodeUtf8.fold(
      e => throw e,
      s => adapter.loadXML(new InputSource(new java.io.ByteArrayInputStream(s.getBytes)), parser)
    )

  implicit class NodeOps(node: Node) {

    def getAttribute(key: String) : String = (node \ s"@$key").text

    def isProperty(key: String) : Boolean = getAttribute("itemprop") == key

    def isClass(key: String) : Boolean = getAttribute("class") == key
  }

  class NodeWrapper(doc: Node) {

    def getTitle(): Seq[String] =
      for /*Seq*/ {
        n <- doc \\ "div"
        if n.isClass("id-app-title")
      } yield n.text.trim

    def getDownloads() : Seq[String] =
      for /*Seq*/ {
        n <- doc \\ "div"
        if n.isProperty("numDownloads")
        if n.isClass("content")
      } yield n.text.trim

    def getStars() : Seq[Double] =
      for /*Seq*/ {
        n <- doc \\ "meta"
        if n.isProperty("ratingValue")
        stars = n.getAttribute("content")
      } yield stars.toDouble

    def getDocId(): Seq[String] =
      (doc \\ "@data-load-more-docid").map(_.text)

    /*We exclude from the URL the portions after the equals symbol, which are parameters
     to choose smaller icons */
    val IconSrcRegex = "//([^=]+)=??.*".r

    def getIcon(): Seq[String] =
      for /*Seq*/ {
        n <- doc \\ "img"
        if n.isProperty("image") 
        url = n.getAttribute("src") match {
          case IconSrcRegex(uri) => uri
        }
      } yield s"http://$url"


    val CategoryHrefRegex = "/store/apps/category/(\\w+)".r

    def getCategories(): Seq[String] =
      for /*Seq*/ {
        n <- doc \\ "a"
        if n.isClass("document-subtitle category")
        c:String = n.getAttribute("href") match {
          case CategoryHrefRegex(cat) => cat
        }
      } yield c

    def isFree(): Seq[Boolean] =
      for /*Seq*/  {
        n <- doc \\ "meta"
        if n.isProperty("price")
        price = n.getAttribute("content").trim
      } yield price == "0" 

    def parseCardAux(): Seq[AppCard] =
      for { /*Seq*/
        docId <- getDocId()
        title <- getTitle()
        free <- isFree()
        icon <- getIcon()
        stars <- getStars()
        downloads <- getDownloads()
      } yield
        AppCard(
          packageName = docId,
          title = title,
          free = free,
          icon = icon,
          stars = stars,
          downloads = downloads,
          categories = getCategories().toList
        )

    def parseCard(): Option[AppCard] = parseCardAux.headOption

    def parseItem(): Option[Item] =
      for { /*Option*/
        title: String <- getTitle().headOption
        docId: String <- getDocId().headOption
        downloads <- getDownloads().headOption
      } yield
        Item(
          DocV2(
            title = title,
            creator = "",
            docid = docId,
            details = Details( appDetails = AppDetails(
              appCategory = getCategories().toList,
              numDownloads = downloads,
              permission = Nil
            ) ),
            aggregateRating = AggregateRating.Zero,
            image = Nil,
            offer = Nil
          )
        )

  }


  def parseItem(byteVector: ByteVector): Option[Item] =
    parseItemAux(decodeNode(byteVector))

  def parseItemAux(document: Node): Option[Item] =
    new NodeWrapper(document).parseItem()

  def parseCard(byteVector: ByteVector): Option[AppCard] =
    parseCardAux(decodeNode(byteVector))

  def parseCardAux(document: Node): Option[AppCard] =
    new NodeWrapper(document).parseCard()

}



