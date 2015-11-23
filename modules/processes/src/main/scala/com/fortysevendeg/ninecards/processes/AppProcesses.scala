package com.fortysevendeg.ninecards.processes

import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.services.free.algebra.AppGooglePlay.AppGooglePlayServices
import com.fortysevendeg.ninecards.services.free.algebra.AppPersistence.AppPersistenceServices

class AppProcesses[F[_]](
  implicit AP: AppPersistenceServices[NineCardsServices],
  AG: AppGooglePlayServices[NineCardsServices]) {

  import AG._
  import AP._

  def categorizeApps(packageNames: Seq[String]) = for {
    persistenceApps <- getCategories(packageNames)
    googlePlayApps <- getCategoriesFromGooglePlay(persistenceApps.notFoundApps)
  } yield (persistenceApps.categorizedApps ++ googlePlayApps.categorizedApps) map toGooglePlayApp
}

object AppProcesses {

  implicit def appProcesses[F[_]](implicit AP: AppPersistenceServices[NineCardsServices], AG: AppGooglePlayServices[NineCardsServices]) = new AppProcesses[F]
}
