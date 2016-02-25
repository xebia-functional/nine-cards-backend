package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import org.http4s._
import org.http4s.Http4s._
import org.http4s.Status.ResponseClass.Successful

import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import cats.~>
import scalaz.concurrent.Task

import scodec.bits.ByteVector
import scala.collection.JavaConversions._
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.googleplay.proto.GooglePlay.ResponseWrapper

import com.fortysevendeg.extracats._
import cats.data.Xor
import cats.syntax.option._
import cats.std.list._
import cats.syntax.traverse._

//todo break this up a bit, maybe different file for the bytevector stuff
object Http4sTaskInterpreter {

  def packageUri(p: Package): Option[Uri] = Uri.fromString(s"https://android.clients.google.com/fdfe/details?doc=${p.value}").toOption

  implicit def protobufItemDecoder(implicit byteVectorDecoder: EntityDecoder[ByteVector]): EntityDecoder[Item] = byteVectorDecoder map parseResponseToItem

  val client = org.http4s.client.blaze.PooledHttp1Client()

  val parseResponseToItem: ByteVector => Item = { byteVector =>

    val docV2 = ResponseWrapper.parseFrom(byteVector.toArray).getPayload.getDetailsResponse.getDocV2
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

  def headers(t: Token, id: AndroidId, lo: Option[Localization]): Headers = {


    val allHeaders = lo.map {
      case Localization(locale) => Header("Accept-Language", locale)
    }.toList ++ List(
      Header("Authorization", s"GoogleLogin auth=${t.value}"),
      Header("X-DFE-Enabled-Experiments", "cl:billing.select_add_instrument_by_default"),
      Header("X-DFE-Unsupported-Experiments", "nocache:billing.use_charging_poller,market_emails,buyer_currency,prod_baseline,checkin.set_asset_paid_app_field,shekel_test,content_ratings,buyer_currency_in_app,nocache:encrypted_apk,recent_changes"),
      Header("X-DFE-Device-Id", id.value),
      Header("X-DFE-Client-Id", "am-android-google"),
      Header("User-Agent", "Android-Finsky/3.10.14 (api=3,versionCode=8016014,sdk=15,device=GT-I9300,hardware=aries,product=GT-I9300)"),
      Header("X-DFE-SmallestScreenWidthDp", "320"),
      Header("X-DFE-Filter-Level", "3"),
      Header("Host", "android.clients.google.com"),
      Header("Content-Type", "application/json; charset=UTF-8")
    )

    Headers(allHeaders: _*)
  }

  // todo it's expected to change Option[Item] to be something more granular to indicate failure
  def request(pkg: Package, h: Headers): Task[Xor[String, Item]] = {

    val packageName = pkg.value

    packageUri(pkg).fold(Task.now(Xor.left(packageName)): Task[Xor[String, Item]]) {u =>
      val request = new Request(
        method = Method.GET,
        uri = u,
        headers = h
      )
      client.fetch(request) {
        case Successful(resp) => resp.as[Item].map(i => Xor.right(i))
        case x => Task.now(Xor.left(packageName))
      }
    }
  }

  implicit val interpreter = new (GooglePlayOps ~> Task) {
    def apply[A](fa: GooglePlayOps[A]) = fa match {
      case RequestPackage((token, androidId, localizationOption), pkg) =>
        request(pkg, headers(token, androidId, localizationOption)).map(_.fold(_ => None: Option[Item], Some(_)))

      case BulkRequestPackage((token, androidId, localizationOption), PackageListRequest(packageNames)) =>
        val packages: List[Package] = packageNames.map(Package.apply)

        val hs: Headers = headers(token, androidId, localizationOption)

        val fetched: Task[List[Xor[String, Item]]] = packages.traverse{ p =>
          request(p, hs)
        }

        fetched.map { (xors: List[Xor[String, Item]]) =>
          xors.foldLeft(PackageDetails(Nil, Nil)) { case (PackageDetails(errors, items), xor) =>
            xor.fold(s => PackageDetails(s :: errors, items), i => PackageDetails(errors, i :: items))
          }
        }
    }
  }
}
