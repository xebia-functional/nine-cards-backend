package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi

import com.fortysevendeg.ninecards.googleplay.domain.{DocV2 => DomainDocV2, _}
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi.proto.GooglePlay.{ListResponse, DocV2}
import scala.collection.JavaConversions._

object Converters {

  def listResponseToPackages(listResponse: ListResponse) : List[String] = {
    for /*List*/ {
      app <- listResponse.getDocList.toList
      doc <- app.getChildList.toList
      docId = doc.getDocid
      if ! docId.isEmpty
    } yield docId
  }

  sealed abstract class ImageType(val index: Int)
  case object Screenshot extends ImageType(1)
  case object Icon extends ImageType(4)

  class WrapperDocV2(docV2: DocV2) {

    private[this] def imageUrls(imageType: ImageType) : List[String] =
      for /*List */ {
        img <- docV2.getImageList.toList
        if img.getImageType == imageType.index
        url = img.getImageUrl.replaceFirst("https","http")
      } yield url

    lazy val docid : String = docV2.getDocid
    lazy val title: String = docV2.getTitle

    lazy val categories: List[String] = docV2.getDetails.getAppDetails.getAppCategoryList.toList
    lazy val numDownloads: String = docV2.getDetails.getAppDetails.getNumDownloads
    lazy val icon: String = imageUrls(Icon).headOption.getOrElse("")
    lazy val isFree: Boolean = docV2.getOfferList.exists(_.getMicros == 0)
    lazy val starRating: Double = docV2.getAggregateRating.getStarRating

    def toAppCard(): AppCard =
      AppCard(
        packageName = docid,
        title = title,
        free = isFree,
        icon = icon,
        stars = starRating,
        downloads = numDownloads,
        categories = categories
      )

    def toAppRecommendation(): AppRecommendation =
      AppRecommendation(
        packageName = docid,
        name = title,
        free = isFree,
        icon = icon,
        stars = starRating,
        downloads = numDownloads,
        screenshots = imageUrls(Screenshot)
      )

    def toItem() : Item = {
      val details = docV2.getDetails
      val appDetails = details.getAppDetails
      val agg = docV2.getAggregateRating

      Item( DomainDocV2(
        title   = title,
        creator = docV2.getCreator,
        docid   = docid,
        details = Details( appDetails = AppDetails(
          appCategory  = appDetails.getAppCategoryList.toList,
          numDownloads = appDetails.getNumDownloads,
          permission   = appDetails.getPermissionList.toList
        )),
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
      ))
    }
  }

  def toItem(docV2: DocV2) : Item = new WrapperDocV2(docV2).toItem

  def toCard(docV2: DocV2) : AppCard = new WrapperDocV2(docV2).toAppCard

  def toAppRecommendation(docV2: DocV2) = new WrapperDocV2(docV2).toAppRecommendation

  def toAppRecommendationList(listResponse: ListResponse) : AppRecommendationList = {
    val docs: List[DocV2] = listResponse.getDoc(0).getChildList().toList
    toAppRecommendationList(docs)
  }

  def toAppRecommendationList( docs: List[DocV2]) : AppRecommendationList = {
    val apps = for /*List*/ {
      doc <- docs
      wr = new WrapperDocV2(doc)
      if (! wr.docid.isEmpty)
      // If a DocV2 corresponds to no app, it is a DefaultInstance and as such has an empty docId
    } yield wr.toAppRecommendation

    AppRecommendationList(apps)
  }

}

