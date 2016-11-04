package cards.nine.googleplay.service.free.interpreter.googleapi

import cards.nine.domain.application.{ FullCard, FullCardList, Package, BasicCard }
import cards.nine.googleplay.proto.GooglePlay.{ ListResponse, DocV2, SearchResponse }
import scala.collection.JavaConversions._

object Converters {

  def listResponseToPackages(listResponse: ListResponse): List[Package] =
    extractPackageList(listResponse.getDocList.toList)

  def searchResponseToPackages(searchResponse: SearchResponse): List[Package] =
    extractPackageList(searchResponse.getDocList.toList)

  private def extractPackageList(docList: List[DocV2]): List[Package] =
    for /*List*/ {
      app ← docList
      doc ← app.getChildList.toList
      docId = doc.getDocid
      if !docId.isEmpty
    } yield Package(docId)

  def listResponseListToPackages(listResponses: List[ListResponse]): List[Package] =
    listResponses.flatMap(listResponseToPackages).distinct

  sealed abstract class ImageType(val index: Int)
  case object Screenshot extends ImageType(1)
  case object Icon extends ImageType(4)

  class WrapperDocV2(docV2: DocV2) {

    private[this] def imageUrls(imageType: ImageType): List[String] =
      for /*List */ {
        img ← docV2.getImageList.toList
        if img.getImageType == imageType.index
        url = img.getImageUrl.replaceFirst("https", "http")
      } yield url

    lazy val docid: String = docV2.getDocid
    lazy val title: String = docV2.getTitle

    lazy val categories: List[String] = docV2.getDetails.getAppDetails.getAppCategoryList.toList
    lazy val numDownloads: String = docV2.getDetails.getAppDetails.getNumDownloads
    lazy val icon: String = imageUrls(Icon).headOption.getOrElse("")
    lazy val isFree: Boolean = docV2.getOfferList.exists(_.getMicros == 0)
    lazy val starRating: Double = docV2.getAggregateRating.getStarRating

    def toFullCard(): FullCard = FullCard(
      packageName = Package(docid),
      title       = title,
      free        = isFree,
      icon        = icon,
      stars       = starRating,
      downloads   = numDownloads,
      screenshots = imageUrls(Screenshot),
      categories  = categories
    )

    def toBasicCard(): BasicCard = BasicCard(
      packageName = Package(docid),
      title       = title,
      free        = isFree,
      icon        = icon,
      stars       = starRating,
      downloads   = numDownloads
    )

  }

  def toFullCard(docV2: DocV2): FullCard = new WrapperDocV2(docV2).toFullCard
  def toBasicCard(docV2: DocV2): BasicCard = new WrapperDocV2(docV2).toBasicCard

  def toFullCardList(docs: List[DocV2]): FullCardList = {
    val apps: List[FullCard] = for /*List*/ {
      doc ← docs
      wr = new WrapperDocV2(doc)
      if (!wr.docid.isEmpty)
      // If a DocV2 corresponds to no app, it is a DefaultInstance and as such has an empty docId
    } yield wr.toFullCard
    FullCardList(List(), apps)
  }

}

