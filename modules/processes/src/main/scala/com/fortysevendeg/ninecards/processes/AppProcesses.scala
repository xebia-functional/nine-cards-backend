package com.fortysevendeg.ninecards.processes

import NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.services.free.algebra.appsGooglePlay.AppGooglePlayServices
import com.fortysevendeg.ninecards.services.free.algebra.appsPersistence.AppPersistenceServices
import com.fortysevendeg.ninecards.processes.converters.Converters._

class AppProcesses[F[_]](
  implicit AP: AppPersistenceServices[NineCardsServices],
  AG: AppGooglePlayServices[NineCardsServices]) {

  import AG._
  import AP._

  def categorizeApps(packageNames: Seq[String]) = {
    for {
      persistenceApps <- getCategories(packageNames)
      googlePlayApps <- getCategoriesFromGooglePlay(persistenceApps.notFoundApps)
    } yield (persistenceApps.categorizedApps ++ googlePlayApps.categorizedApps) map toGooglePlayApp
  }
}
