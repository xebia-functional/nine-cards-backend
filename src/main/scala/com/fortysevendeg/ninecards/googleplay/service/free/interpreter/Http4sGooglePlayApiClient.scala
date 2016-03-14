package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import org.http4s._
import org.http4s.Http4s._
import org.http4s.Status.ResponseClass.Successful
import scodec.bits.ByteVector
import scala.collection.JavaConversions._
import com.fortysevendeg.googleplay.proto.GooglePlay.ResponseWrapper
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import cats.data.Xor
import scalaz.concurrent.Task
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._

class Http4sGooglePlayApiClient(url: String) {

  val client = org.http4s.client.blaze.PooledHttp1Client() // todo where is best to create the client?

  def packageUri(p: Package): Option[Uri] = Uri.fromString(s"$url?doc=${p.value}").toOption

  implicit def protobufItemDecoder(implicit byteVectorDecoder: EntityDecoder[ByteVector]): EntityDecoder[Item] = byteVectorDecoder map parseResponseToItem

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
        image = Nil,
        offer = Nil
      )
    )
  }

  def headers(auth: GoogleAuthParams): Headers = {

    val (t, id, lo) = auth

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

  def request(pkg: Package, auth: GoogleAuthParams): Task[Xor[String, Item]] = {

    val packageName = pkg.value

    val h = headers(auth)

    packageUri(pkg).fold(Task.now(Xor.left(packageName)): Task[Xor[String, Item]]) {u =>
      val request = new Request(
        method = Method.GET,
        uri = u,
        headers = h
      )
      client.fetch(request) {
        case Successful(resp) => resp.as[Item].map(i => Xor.right(i))
        case _ => Task.now(Xor.left(packageName))
      }.handle {
        case _ => Xor.left(packageName)
      }
    }
  }
}
