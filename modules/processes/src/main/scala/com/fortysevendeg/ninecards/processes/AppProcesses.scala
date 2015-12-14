package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.domain.GooglePlayApp
import com.fortysevendeg.ninecards.services.free.algebra.AppGooglePlay.AppGooglePlayServices
import com.fortysevendeg.ninecards.services.free.algebra.AppPersistence.AppPersistenceServices

import scala.language.higherKinds

class AppProcesses[F[_]](
  implicit appPersistenceServices: AppPersistenceServices[F],
  appGooglePlayServices: AppGooglePlayServices[F]) {

  import appGooglePlayServices._
  import appPersistenceServices._

  def categorizeApps(packageNames: Seq[String]): Free[F, Seq[GooglePlayApp]] = for {
    persistenceApps <- getCategories(packageNames)
    googlePlayApps <- getCategoriesFromGooglePlay(persistenceApps.notFoundApps)
  } yield (persistenceApps.categorizedApps ++ googlePlayApps.categorizedApps) map toGooglePlayApp

}

object AppProcesses {

  implicit def appProcesses[F[_]](implicit AP: AppPersistenceServices[F], AG: AppGooglePlayServices[F]) = new AppProcesses()
}
