package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.config.NineCardsConfig
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._
import scala.xml.Node
import cats.data.Xor
import cats.std.option._
import cats.syntax.cartesian._
import cats.syntax.xor._
import scalaz.concurrent.Task
import org.http4s._
import org.http4s.Http4s._
import scodec.bits.ByteVector
import scala.xml.parsing.NoBindingFactoryAdapter
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.xml.sax.InputSource
import org.http4s.Status.ResponseClass.Successful

class Http4sGooglePlayWebScraper(url: String) {

  val parser = new SAXFactoryImpl().newSAXParser()
  val adapter = new NoBindingFactoryAdapter

  val byteVectorToEntityDecodedNode: ByteVector => Node = { byteVector =>
    byteVector.decodeUtf8.fold ({ e =>
      throw e
    }, {s =>
      adapter.loadXML(new InputSource(new java.io.ByteArrayInputStream(s.getBytes)), parser)
    })
  }

  implicit def httpNodeDecoder(implicit byteVectorDecoder: EntityDecoder[ByteVector]): EntityDecoder[Node] = byteVectorDecoder map byteVectorToEntityDecodedNode

  def parseResponseToItem(document: Node): Option[Item] = {

    val parsedTitle: Option[String] = {
      val collected = (document \\ "div").collect {
        case n if((n \\ "@itemprop").text == "name") => n.child.text.trim
      }

      collected.headOption
    }

    val parsedDocId: Option[String] = {
      val collected = (document \\ "@data-load-more-docid").collect {
        case n => n.text
      }

      collected.headOption
    }

    val CategoryHrefRegex = "/store/apps/category/(\\w+)".r

    val parsedAppCategories: List[String] = {
      def spanName(n: Node): String = {
        val spanElem = (n \\ "span")
        (spanElem \\ "@itemprop").text
      }

      val categories: Seq[String] = (document \\ "a") collect {
        case n if (spanName(n) == "genre") =>
          val href = (n \\ "@href").text
          href match {
            case CategoryHrefRegex(path) => path
          }
      }

      categories.toList
    }

    (parsedTitle |@| parsedDocId).map {(title, docId) => 
      Item(
        DocV2(
          title = title,
          creator = "",
          docid = docId,
          details = Details(
            appDetails = AppDetails(
              appCategory = parsedAppCategories,
              numDownloads = "",
              permission = Nil
            )
          ),
          aggregateRating = AggregateRating(
            ratingsCount = 0,
            oneStarRatings = 0,
            twoStarRatings = 0,
            threeStarRatings = 0,
            fourStarRatings = 0,
            fiveStarRatings = 0,
            starRating = 0.0
          ),
          image = Nil,
          offer = Nil
        )
      )
    }
  }

  def request(p: Package, localizationOption: Option[Localization]): Task[Xor[String, Item]] = {
    val client = org.http4s.client.blaze.PooledHttp1Client()

    val localization = localizationOption.fold("")(l => s"&hl=${l.value}")

    def packageUri(p: Package): Option[Uri] = Uri.fromString(s"$url?id=${p.value}${localization}").toOption

    packageUri(p).fold(Task.now(p.value.left[Item])) {u =>
      val request = new Request(
        method = Method.GET,
        uri = u
      )
      client.fetch(request) {
        case Successful(resp) => resp.as[Node].map{ n =>
          val maybeItem = parseResponseToItem(n)
          maybeItem.fold(p.value.left[Item])(i => i.right[String])
        }
        case x => Task.now(p.value.left[Item])
      }
    }
  }
}
