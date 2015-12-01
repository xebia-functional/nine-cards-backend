package com.fortysevendeg.ninecards.services.free.interpreter.impl

import com.fortysevendeg.ninecards.services.free.domain.{GooglePlayApp, CategorizeResponse}

class AppGooglePlayImpl {

  def getCategoriesFromGooglePlay(packageNames: Seq[String]) =
    CategorizeResponse(
      categorizedApps = Seq(GooglePlayApp(
        packageName = "com.android.chrome",
        appType = "",
        appCategory = "COMMUNICATION",
        numDownloads = "1,000,000,000 - 5,000,000,000",
        starRating = 4.235081672668457,
        ratingCount = 3715846,
        commentCount = 0)),
      notFoundApps = Seq("com.facebook.orca"))

}

object AppGooglePlayImpl {

  implicit def appGooglePlayImpl = new AppGooglePlayImpl()
}