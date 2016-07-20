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

  private[this] val namePF: PartialFunction[Node, String] = {
    case n if((n \\ "@itemprop").text == "name") => n.child.text.trim
  }

  private[this] val docIdPF: PartialFunction[Node, String] = {
    case n => n.text
  }

  private[this] val CategoryHrefRegex = "/store/apps/category/(\\w+)".r

  private def spanName(n: Node): String = ((n \\ "span") \\ "@itemprop").text

  private[this] val categoryPF: PartialFunction[Node, String] = {
    case n if (spanName(n) == "genre") =>
      val href = (n \\ "@href").text
      href match {
        case CategoryHrefRegex(path) => path
      }
  }

  private[this] def simpleItem(title:String, docId: String, categories: List[String]): Item =
    Item(
      DocV2(
        title = title,
        creator = "",
        docid = docId,
        details = Details(
          appDetails = AppDetails(appCategory = categories, numDownloads = "", permission = Nil)
        ),
        aggregateRating = AggregateRating.Zero,
        image = Nil,
        offer = Nil
      )
    )

  private[this] def simpleAppCard(title:String, docId: String, categories: List[String]): AppCard =
    AppCard(
      packageName = docId,
      title = title,
      free = false, // TODO
      icon = "//TODO", //TODO
      stars = 0.0, //TODO
      downloads = "//TODO", //TODO,
      categories = categories
    )

  private val parser = new SAXFactoryImpl().newSAXParser()
  private val adapter = new NoBindingFactoryAdapter

  private def decodeNode( byteVector: ByteVector): Node =
    byteVector.decodeUtf8.fold(
      e => throw e,
      s => adapter.loadXML(new InputSource(new java.io.ByteArrayInputStream(s.getBytes)), parser)
    )

  def parseItem(byteVector: ByteVector): Option[Item] =
    parseItemAux(decodeNode(byteVector))

  def parseItemAux(document: Node): Option[Item] =
    for { /*Option*/
      title: String <- (document \\ "div").collect(namePF).headOption
      docId: String <- (document \\ "@data-load-more-docid").collect(docIdPF).headOption
      categories: List[String] = (document \\ "a").collect(categoryPF).toList
    } yield simpleItem(title, docId, categories)

  def parseCard(byteVector: ByteVector): Option[AppCard] =
    parseCardAux(decodeNode(byteVector))

  def parseCardAux(document: Node): Option[AppCard] =
    for { /*Option*/
      title: String <- (document \\ "div").collect(namePF).headOption
      docId: String <- (document \\ "@data-load-more-docid").collect(docIdPF).headOption
      categories: List[String] = (document \\ "a").collect(categoryPF).toList
    } yield simpleAppCard(title, docId, categories)

}