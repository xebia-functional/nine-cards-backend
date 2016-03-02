package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import scala.xml.Node
import cats.std.option._
import cats.syntax.cartesian._

object Http4sGooglePlayWebScraper {
  def parseResponseToItem(document: Node): Option[Item] = {

    val CategoryHrefRegex = "/store/apps/category/(\\w+)".r

    val parsedTitle: Option[String] = {
      val collected = (document \\ "div").collect {
        case n if((n \ "@class").text == "id-app-title") => n.text
      }

      collected.headOption
    }

    val parsedDocId: Option[String] = {
      val collected = (document \\ "div").collect{
        case n if((n \ "@class").text == "details-wrapper apps square-cover id-track-partial-impression id-deep-link-item") =>
          (n \ "@data-docid").text
      }

      collected.headOption
    }

    val parsedAppCategories: List[String] = {
      val categories: Seq[String] = (document \\ "a") collect {
      case n if (n \\ "@class").text == "document-subtitle category" =>
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
          creator = "Creator",
          docid = docId,
          details = Details(
            appDetails = AppDetails(
              appCategory = parsedAppCategories,
              numDownloads = "Lots",
              permission = List()
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
          image = List(),
          offer = List()
        )
      )
    }
  }
}
