package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.domain.GooglePlayApp
import com.fortysevendeg.ninecards.services.common.TaskOps._
import com.fortysevendeg.ninecards.services.free.algebra.AppGooglePlay.AppGooglePlayServices
import com.fortysevendeg.ninecards.services.free.algebra.AppPersistence.AppPersistenceServices
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBOps
import com.fortysevendeg.ninecards.services.persistence.PersistenceTest.PersistenceServices
import com.fortysevendeg.ninecards.services.persistence._
import doobie.imports._

import scala.language.higherKinds

class AppProcesses[F[_]](
  implicit appPersistenceServices: AppPersistenceServices[F],
  appGooglePlayServices: AppGooglePlayServices[F],
  dbOps: DBOps[F],
  persistenceServices: PersistenceServices) {

  def categorizeApps(packageNames: Seq[String]): Free[F, Seq[GooglePlayApp]] = {

    val databaseOps = for {
      _ <- persistenceServices.dropTable
      _ <- persistenceServices.createTable
      itemId <- persistenceServices.addItem("Test1")
      fetchItem <- persistenceServices.getItem(itemId)
    } yield fetchItem

    for {
      persistenceApps <- appPersistenceServices.getCategories(packageNames)
      googlePlayApps <- appGooglePlayServices.getCategoriesFromGooglePlay(persistenceApps.notFoundApps)
      item <- databaseOps.transact(transactor)
    } yield (persistenceApps.categorizedApps ++ googlePlayApps.categorizedApps) map toGooglePlayApp
  }

}

object AppProcesses {

  implicit def appProcesses[F[_]](
    implicit appPersistenceServices: AppPersistenceServices[F],
    appGooglePlayServices: AppGooglePlayServices[F],
    dbOps: DBOps[F],
    persistenceServices: PersistenceServices) = new AppProcesses()
}
