package com.fortysevendeg.ninecards.googleplay.service

import com.fortysevendeg.ninecards.api.Domain._
import com.akdeniz.googleplaycrawler.GooglePlayException
import cats.data.Xor
import GooglePlayService._
import com.akdeniz.googleplaycrawler.GooglePlayAPI
import scala.collection.JavaConversions._
import org.apache.http.impl.client.DefaultHttpClient

object GooglePlayCrawler extends GooglePlayService {
  def packageRequest(params: GoogleAuthParams): Package => Xor[GooglePlayException, Item] = {
    val (t, id, lo) = params
    val gpApi = new GooglePlayAPI()
    gpApi.setToken(t.value)
    gpApi.setAndroidID(id.value)
    gpApi.setClient(new DefaultHttpClient)
    lo.foreach(l => gpApi.setLocalization(l.value))

    { pkg =>

      Xor.catchOnly[GooglePlayException] {
        val docV2 = gpApi.details(pkg.value).getDocV2
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
  }
}
