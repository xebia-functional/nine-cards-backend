package com.fortysevendeg.ninecards.googleplay.domain

import enumeratum.{Enum, EnumEntry}

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

case class InfoError( message: String ) extends AnyVal


sealed trait PriceFilter extends EnumEntry
object PriceFilter extends Enum[PriceFilter] {
  case object ALL extends PriceFilter
  case object FREE extends PriceFilter
  case object PAID extends PriceFilter

  val values = super.findValues
}

case class FullCard(
  packageName: String,
  title: String,
  categories: List[String],
  downloads: String,
  free: Boolean,
  icon: String,
  screenshots: List[String],
  stars: Double
)

case class FullCardList(
  missing: List[String],
  cards: List[FullCard]
)
