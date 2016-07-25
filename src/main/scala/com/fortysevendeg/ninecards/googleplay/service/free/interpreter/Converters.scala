package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain._
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.xml.sax.InputSource
import scala.collection.JavaConversions._
import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter
import scodec.bits.ByteVector

object GoogleApiItemParser {

  import com.fortysevendeg.googleplay.proto.{GooglePlay => Proto}

  private def toItem(docV2: Proto.DocV2) : Item = {
    val details = docV2.getDetails
    val appDetails = details.getAppDetails
    val agg = docV2.getAggregateRating

    Item(
      DocV2(
        title   = docV2.getTitle,
        creator = docV2.getCreator,
        docid   = docV2.getDocid,
        details = Details(
          appDetails = AppDetails(
            appCategory  = appDetails.getAppCategoryList.toList,
            numDownloads = appDetails.getNumDownloads,
            permission   = appDetails.getPermissionList.toList
          )
        ),
        aggregateRating = AggregateRating(
          ratingsCount     = agg.getRatingsCount,
          oneStarRatings   = agg.getOneStarRatings,
          twoStarRatings   = agg.getTwoStarRatings,
          threeStarRatings = agg.getThreeStarRatings,
          fourStarRatings  = agg.getFourStarRatings,
          fiveStarRatings  = agg.getFiveStarRatings,
          starRating       = agg.getStarRating
        ),
        image = Nil,
        offer = Nil
      )
    )
  }


  private def toAppCard(docV2: Proto.DocV2) : AppCard = {
    val appDetails = docV2.getDetails.getAppDetails
    AppCard(
      packageName = docV2.getDocid,
      title   = docV2.getTitle,
      free = false, // TODO
      icon = "", // TODO
      stars = docV2.getAggregateRating.getStarRating,
      downloads = appDetails.getNumDownloads,
      categories = appDetails.getAppCategoryList.toList
    )
  }

  def parseItem(byteVector: ByteVector) : Item = {
    val docV2 = Proto.ResponseWrapper.parseFrom(byteVector.toArray).getPayload.getDetailsResponse.getDocV2
    toItem(docV2)
  }

  def parseCard(byteVector: ByteVector) : AppCard = {
    val docV2 = Proto.ResponseWrapper.parseFrom(byteVector.toArray).getPayload.getDetailsResponse.getDocV2
    toAppCard(docV2)
  }
}



object GooglePlayPageParser {

  private val parser = new SAXFactoryImpl().newSAXParser()
  private val adapter = new NoBindingFactoryAdapter

  private def decodeNode( byteVector: ByteVector): Node =
    byteVector.decodeUtf8.fold(
      e => throw e,
      s => adapter.loadXML(new InputSource(new java.io.ByteArrayInputStream(s.getBytes)), parser)
    )

  implicit class NodeOps(node: Node) {

    def getAttribute(key: String) : String = (node \\ s"@$key").text

    def isProperty(key: String) : Boolean = getAttribute("itemprop") == key

    def isClass(key: String) : Boolean = getAttribute("class") == key
  }

  class NodeWrapper(doc: Node) {

    def getTitle(): Seq[String] =
      for /*Seq*/ {
        n <- doc \\ "div"
        if n.isProperty("name")
        name = n.child.text.trim
      } yield name

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
      for { /*Option*/
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



