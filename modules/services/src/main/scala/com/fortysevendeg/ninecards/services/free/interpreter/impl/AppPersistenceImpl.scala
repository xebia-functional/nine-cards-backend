package com.fortysevendeg.ninecards.services.free.interpreter.impl

import com.fortysevendeg.ninecards.services.free.domain.{CategorizeResponse, GooglePlayApp}

class AppPersistenceImpl {

  def getCategories(packageNames: Seq[String]) =
    CategorizeResponse(
      categorizedApps = Seq(GooglePlayApp(
        packageName = "com.whatsapp",
        appType = "",
        appCategory = "COMMUNICATION",
        numDownloads = "1,000,000,000 - 5,000,000,000",
        starRating = 4.433322429656982,
        ratingCount = 31677777,
        commentCount = 0)),
      notFoundApps = Seq("com.skype.raider"))

  def saveCategories(packageNames: Seq[String]): Seq[String] =
    Seq("GAMES")
}

object AppPersistenceImpl {

  implicit def appPersistenceImpl = new AppPersistenceImpl()
}