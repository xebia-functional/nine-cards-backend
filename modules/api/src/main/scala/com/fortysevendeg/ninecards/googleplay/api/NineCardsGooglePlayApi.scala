package com.fortysevendeg.ninecards.api

import cats.data.Xor
import com.akdeniz.googleplaycrawler.GooglePlayException
import spray.routing._
import akka.actor.Actor
import com.akdeniz.googleplaycrawler.GooglePlayAPI
import org.apache.http.impl.client.DefaultHttpClient
import scala.collection.JavaConversions._
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

class NineCardsGooglePlayActor extends Actor with NineCardsGooglePlayApi {

  def actorRefFactory = context

  def receive = runRoute(googlePlayApiRoute)
}

object Domain {
  case class Token(value: String) extends AnyVal
  case class AndroidId(value: String) extends AnyVal
  case class Localisation(value: String) extends AnyVal

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
  def googlePlayApiRoute: Route = packageRoute

  type Headers = Token :: AndroidId :: Option[Localisation] :: HNil

  val requestHeaders: Directive[Headers] = for {
    token        <- headerValueByName("X-Google-Play-Token")
    androidId    <- headerValueByName("X-Android-ID")
    localisation <- optionalHeaderValueByName("X-Android-Market-Localization")
  } yield Token(token) :: AndroidId(androidId) :: localisation.map(Localisation.apply) :: HNil

  // todo I should be able to make this generic and move it into the package object
  implicit val packageListUnmarshaller: Unmarshaller[PackageListRequest] = new Unmarshaller[PackageListRequest] {
    def apply(entity: HttpEntity) = {
      decode[PackageListRequest](entity.asString).fold(e => Left(MalformedContent("Unable to parse entity into JSON list", e)), s => Right(s))
    }
  }


  // TODO: Turn package into a real type
  // TODO: Have this run async
  private[this] def getPackage(t: Token, id: AndroidId, lo: Option[Localisation], packageName: String): Xor[GooglePlayException, Item] = {
    val gpApi = new GooglePlayAPI()
    gpApi.setToken(t.value)
    gpApi.setAndroidID(id.value)
    gpApi.setClient(new DefaultHttpClient)
    lo.foreach(l => gpApi.setLocalization(l.value))

    Xor.catchOnly[GooglePlayException] {
      val docV2 = gpApi.details(packageName).getDocV2
      val details = docV2.getDetails
      val appDetails = details.getAppDetails
      val agg = docV2.getAggregateRating

      Item(
        DocV2(
          title   = docV2.getTitle,
          creator = docV2.getCreator,
          docid   = docV2.getDocid,
          details = Details(
            appDetails = AppDetails(
              appCategory  = appDetails.getAppCategoryList.toList,
              numDownloads = appDetails.getNumDownloads,
              permission   = appDetails.getPermissionList.toList
            )
          ),
          aggregateRating = AggregateRating(
            ratingsCount     = agg.getRatingsCount,
            oneStarRatings   = agg.getOneStarRatings,
            twoStarRatings   = agg.getTwoStarRatings,
            threeStarRatings = agg.getThreeStarRatings,
            fourStarRatings  = agg.getFourStarRatings,
            fiveStarRatings  = agg.getFiveStarRatings,
            starRating       = agg.getStarRating
          ),
          image = List(), // TODO
          offer = List()  // TODO
        )
      )
    }
  }

  private[this] def packageRoute =
    pathPrefix("googleplay") {
      requestHeaders { (token, androidId, localisationOption) =>
        get {
          path("package" / Segment) { packageName => // TODO make this a package type
            val packageDetails = getPackage(token, androidId, localisationOption, packageName)
            complete(packageDetails)
          }
        } ~
        post {
          path("packages" / "detailed") {
            entity(as[PackageListRequest]) { case PackageListRequest(packageNames) =>

              val details = packageNames.foldLeft(PackageDetails(Nil, Nil)) { case (PackageDetails(errors, items), packageName) =>
                val xOrPackage = getPackage(token, androidId, localisationOption, packageName)
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
