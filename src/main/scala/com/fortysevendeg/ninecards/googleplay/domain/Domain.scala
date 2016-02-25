package com.fortysevendeg.ninecards.googleplay.domain

import spray.httpx.unmarshalling.MalformedContent
import spray.httpx.unmarshalling.Unmarshaller
import spray.httpx.marshalling.ToResponseMarshaller
import spray.http.{HttpEntity, HttpResponse, StatusCodes}
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._

object Domain {
  case class Package(value: String) extends AnyVal

  case class PackageListRequest(items: List[String]) extends AnyVal
  case class PackageDetails(errors: List[String], items: List[Item])

  case class Item(docV2: DocV2)
  case class DocV2(title: String, creator: String, docid: String, details: Details, aggregateRating: AggregateRating, image: List[Image], offer: List[Offer])
  case class Details(appDetails: AppDetails)
  case class AppDetails(appCategory: List[String], numDownloads: String, permission: List[String])
  case class AggregateRating(ratingsCount: Long, oneStarRatings: Long, twoStarRatings: Long, threeStarRatings: Long, fourStarRatings: Long, fiveStarRatings: Long, starRating: Double) // commentcount?
  case class Image(imageType: Long, imageUrl: String) // todo check which fields are necessary here
  case class Offer(offerType: Long) // todo check which fields are necessary here

  // Domain-specific marshalling and unmarshalling
  implicit val packageListUnmarshaller: Unmarshaller[PackageListRequest] = new Unmarshaller[PackageListRequest] {
    def apply(entity: HttpEntity) = {
      decode[PackageListRequest](entity.asString).fold(e => Left(MalformedContent(s"Unable to parse entity into JSON list: $entity", e)), s => Right(s))
    }
  }

  implicit def optionalItemMarshaller(implicit im: ToResponseMarshaller[Item]): ToResponseMarshaller[Option[Item]] = {
    ToResponseMarshaller[Option[Item]] { (o, ctx) =>
      o.fold(ctx.marshalTo(HttpResponse(status = StatusCodes.InternalServerError, entity = HttpEntity("Cannot find item!"))))(im(_, ctx))
    }
  }
}

