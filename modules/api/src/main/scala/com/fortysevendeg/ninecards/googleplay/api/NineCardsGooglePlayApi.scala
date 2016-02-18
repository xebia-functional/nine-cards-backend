package com.fortysevendeg.ninecards.api // todo this package name is wrong

import cats.data.Xor
import com.akdeniz.googleplaycrawler.GooglePlayException
import spray.routing._
import akka.actor.Actor



import shapeless._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import com.fortysevendeg.ninecards.googleplay.ninecardsspray._
import Domain._

import spray.httpx.unmarshalling.MalformedContent
import spray.httpx.unmarshalling.Unmarshaller
import spray.http.HttpEntity

import com.fortysevendeg.ninecards.googleplay.service.GooglePlayService._
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayCrawler

class NineCardsGooglePlayActor extends Actor with NineCardsGooglePlayApi {

  def actorRefFactory = context

  def receive = runRoute(googlePlayApiRoute(GooglePlayCrawler.packageRequest _))
}

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
}

trait NineCardsGooglePlayApi extends HttpService {

  //type Headers = Token :: AndroidId :: Option[Localisation] :: HNil

  val requestHeaders = for {
    token        <- headerValueByName("X-Google-Play-Token")
    androidId    <- headerValueByName("X-Android-ID")
    localisation <- optionalHeaderValueByName("X-Android-Market-Localization")
  } yield Token(token) :: AndroidId(androidId) :: localisation.map(Localization.apply) :: HNil

  // todo I should be able to make this generic and move it into the package object
  implicit val packageListUnmarshaller: Unmarshaller[PackageListRequest] = new Unmarshaller[PackageListRequest] {
    def apply(entity: HttpEntity) = {
      decode[PackageListRequest](entity.asString).fold(e => Left(MalformedContent("Unable to parse entity into JSON list", e)), s => Right(s))
    }
  }

  def googlePlayApiRoute(getPackage: (GoogleAuthParams) => Package => Xor[GooglePlayException, Item]) =
    pathPrefix("googleplay") {
      requestHeaders { (token, androidId, localisationOption) =>
        get {
          path("package" / Segment) { packageName => // TODO make this a package type
            val packageDetails = getPackage((token, androidId, localisationOption))(Package(packageName))
            complete(packageDetails)
          }
        } ~
        post {
          path("packages" / "detailed") {
            entity(as[PackageListRequest]) { case PackageListRequest(packageNames) =>

              val packageFetcher = getPackage((token, androidId, localisationOption))

              val details = packageNames.foldLeft(PackageDetails(Nil, Nil)) { case (PackageDetails(errors, items), packageName) =>
                val xOrPackage = packageFetcher(Package(packageName))
                // todo is it worth logging errors here?
                xOrPackage.fold(_ => PackageDetails(packageName :: errors, items), p => PackageDetails(errors, p :: items))
              }

              complete(details)
            }
          }
        }
      }
    }
}
