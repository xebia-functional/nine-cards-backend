package com.fortysevendeg.ninecards.services.free.domain

case class GooglePlayApp(
  packageName: String,
  appType: String,
  appCategory: String,
  numDownloads: String,
  starRating: Double,
  ratingCount: Int,
  commentCount: Int
)

object GooglePlay {

  case class AuthParams(
    androidId: String,
    localization: Option[String],
    token: String
  )

  case class PackageListRequest(items: Seq[String]) extends AnyVal

  // These classes are the messages issued in the response of GooglePlay backend,
  // and should not be changed apart from there.

  case class AppsDetails(errors: List[String], items: List[App])
  case class App(docV2: DocV2)
  case class DocV2(
    title: String,
    creator: String,
    docid: String,
    details: Details,
    aggregateRating: AggregateRating,
    image: List[Image],
    offer: List[Offer]
  )

  case class Details(appDetails: AppDetails)

  case class AppDetails(
    appCategory: List[String],
    numDownloads: String,
    permission: List[String]
  )

  case class AggregateRating(
    ratingsCount: Long,
    oneStarRatings: Long,
    twoStarRatings: Long,
    threeStarRatings: Long,
    fourStarRatings: Long,
    fiveStarRatings: Long,
    starRating: Double
  )

  case class Image(
    imageType: Long,
    imageUrl: String
  )

  case class Offer(offerType: Long)

  type Failure = String

}

