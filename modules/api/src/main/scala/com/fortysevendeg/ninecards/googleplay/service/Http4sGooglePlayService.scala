package com.fortysevendeg.ninecards.googleplay.service

import GooglePlayService._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.googleplay.proto.GooglePlay.ResponseWrapper
import org.http4s._
import org.http4s.Http4s._
import org.http4s.Status.ResponseClass.Successful
import scalaz.concurrent.Task
import scodec.bits.ByteVector
import scala.collection.JavaConversions._

object Http4sGooglePlayService {

  val client = org.http4s.client.blaze.PooledHttp1Client()

  def packageRequest(params: GoogleAuthParams): Package => Task[Option[Item]] = { p =>

    val (Token(token), AndroidId(androidId), localizationOption) = params

    val headers = List(
      Header("Authorization", s"GoogleLogin auth=$token"),
      Header("X-DFE-Enabled-Experiments", "cl:billing.select_add_instrument_by_default"),
      Header("X-DFE-Unsupported-Experiments", "nocache:billing.use_charging_poller,market_emails,buyer_currency,prod_baseline,checkin.set_asset_paid_app_field,shekel_test,content_ratings,buyer_currency_in_app,nocache:encrypted_apk,recent_changes"),
      Header("X-DFE-Device-Id", androidId),
      Header("X-DFE-Client-Id", "am-android-google"),
      Header("User-Agent", "Android-Finsky/3.10.14 (api=3,versionCode=8016014,sdk=15,device=GT-I9300,hardware=aries,product=GT-I9300)"),
      Header("X-DFE-SmallestScreenWidthDp", "320"),
      Header("X-DFE-Filter-Level", "3"),
      Header("Host", "android.clients.google.com"),
      Header("Content-Type", "application/json; charset=UTF-8")
    )

    val allHeaders = localizationOption.map {
      case Localization(locale) => Header("Accept-Language", locale)
    }.toList ++ headers

    val request = new Request(
      method = Method.GET,
      uri = packageUri(p),
      headers = Headers(allHeaders: _*)
    )

    client.fetch(request) {
      case Successful(resp) => resp.as[Item].map(Some(_))
      case x => Task.now(None)
    }
  }

  def packageUri(p: Package) = Uri.fromString(s"https://android.clients.google.com/fdfe/details?doc=${p.value}").getOrElse(throw new RuntimeException("Nope")) // todo fix this!

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
        image = List(), // TODO
        offer = List()  // TODO
      )
    )
  }
}
