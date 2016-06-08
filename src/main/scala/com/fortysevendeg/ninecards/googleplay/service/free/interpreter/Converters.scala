package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.std.option._
import cats.syntax.cartesian._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.http4s.EntityDecoder
import org.xml.sax.InputSource
import scala.collection.JavaConversions._
import scala.xml.Node
import scala.xml.parsing.NoBindingFactoryAdapter
import scodec.bits.ByteVector

object ItemParser extends (ByteVector => Item) {

  import com.fortysevendeg.googleplay.proto.{GooglePlay => Proto}

  private def javaToScala(docV2: Proto.DocV2) : Item = {
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

  def apply( byteVector: ByteVector) : Item = {
    val docV2 = Proto.ResponseWrapper.parseFrom(byteVector.toArray).getPayload.getDetailsResponse.getDocV2
    javaToScala(docV2)
  }
}

object ByteVectorToItemParser extends (ByteVector => Option[Item]){

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

  private val parser = new SAXFactoryImpl().newSAXParser()
  private val adapter = new NoBindingFactoryAdapter

  private def decodeNode( byteVector: ByteVector): Node =
    byteVector.decodeUtf8.fold(
      e => throw e,
      s => adapter.loadXML(new InputSource(new java.io.ByteArrayInputStream(s.getBytes)), parser)
    )

  def parseItem(document: Node): Option[Item] = {
    val title: Option[String] = (document \\ "div").collect(namePF).headOption
    val docId: Option[String] = (document \\ "@data-load-more-docid").collect(docIdPF).headOption
    val categories: List[String] = (document \\ "a").collect(categoryPF).toList
    (title |@| docId).map(simpleItem(_, _, categories))
  }

  def apply(bv: ByteVector) : Option[Item] = parseItem(decodeNode(bv))

}

object ItemDecoders {

  implicit def itemOptionDecoder(implicit base: EntityDecoder[ByteVector] ) : EntityDecoder[Option[Item]] =
    base map ByteVectorToItemParser

  implicit def protobufItemDecoder(implicit base: EntityDecoder[ByteVector]): EntityDecoder[Item] =
    base map ItemParser

}