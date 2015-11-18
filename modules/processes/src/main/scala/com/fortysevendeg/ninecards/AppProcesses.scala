package com.fortysevendeg.ninecards

import com.fortysevendeg.ninecards.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.free.algebra.appsGooglePlay.AppGooglePlayServices
import com.fortysevendeg.ninecards.free.algebra.appsPersistence.AppPersistenceServices

class AppProcesses[F[_]](
  implicit AP: AppPersistenceServices[NineCardsServices],
  AG: AppGooglePlayServices[NineCardsServices]) {

  import AG._
  import AP._

  def categorizeApps(packageNames: Seq[String]) = {
    for {
      values <- getCategories(packageNames)
      valuesGooglePlay <- getCategoriesFromGooglePlay(packageNames)
    } yield values ++ valuesGooglePlay
  }
}
