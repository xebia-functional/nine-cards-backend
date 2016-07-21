package com.fortysevendeg.ninecards.googleplay.domain

case class Package(value: String) extends AnyVal
case class AndroidId(value: String) extends AnyVal
case class Token(value: String) extends AnyVal
case class Localization(value: String) extends AnyVal

case class GoogleAuthParams(
  androidId: AndroidId,
  token: Token,
  localization: Option[Localization]
)

case class AppRequest(
  packageName: Package,
  authParams: GoogleAuthParams
)

case class PackageList(items: List[String]) extends AnyVal

case class PackageDetails(errors: List[String], items: List[Item])

case class Item(docV2: DocV2)
case class DocV2(title: String, creator: String, docid: String, details: Details, aggregateRating: AggregateRating, image: List[Image], offer: List[Offer])
case class Details(appDetails: AppDetails)
case class AppDetails(appCategory: List[String], numDownloads: String, permission: List[String])
case class AggregateRating(ratingsCount: Long, oneStarRatings: Long, twoStarRatings: Long, threeStarRatings: Long, fourStarRatings: Long, fiveStarRatings: Long, starRating: Double) // commentcount?

object AggregateRating {
  val Zero = AggregateRating(0,0,0,0,0,0,0.0)
}
case class Image(imageType: Long, imageUrl: String) // todo check which fields are necessary here
case class Offer(offerType: Long) // todo check which fields are necessary here

case class AppCard(
  packageName: String,
  title: String,
  free: Boolean,
  icon: String,
  stars: Double,
  downloads: String,
  categories: List[String]
)

case class AppCardList(
  missing: List[String],
  appsCards: List[AppCard]
)

