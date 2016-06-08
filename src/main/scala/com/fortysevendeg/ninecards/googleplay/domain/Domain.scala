package com.fortysevendeg.ninecards.googleplay.domain

import cats.Monoid

object Domain {
  case class Package(value: String) extends AnyVal

  case class PackageListRequest(items: List[String]) extends AnyVal
  case class PackageDetails(errors: List[String], items: List[Item])

  implicit val packageDetailsMoniod: Monoid[PackageDetails] = new Monoid[PackageDetails] {
    def empty: PackageDetails = PackageDetails(Nil, Nil)
    def combine(x: PackageDetails, y: PackageDetails): PackageDetails = PackageDetails(errors = x.errors ++ y.errors, items = x.items ++ y.items)
  }

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

}

