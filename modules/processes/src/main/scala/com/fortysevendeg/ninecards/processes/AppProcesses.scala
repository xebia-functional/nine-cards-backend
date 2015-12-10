package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.domain.{User, GooglePlayApp}
import com.fortysevendeg.ninecards.services.free.algebra.AppGooglePlay.AppGooglePlayServices
import com.fortysevendeg.ninecards.services.free.algebra.AppPersistence.AppPersistenceServices
import com.fortysevendeg.ninecards.services.free.algebra.Users._

import scala.language.higherKinds

class AppProcesses(
  implicit AP: AppPersistenceServices[NineCardsServices],
  AG: AppGooglePlayServices[NineCardsServices],
  AU: UserServices[NineCardsServices])
{

  import AG._
  import AP._
  import AU._

  def categorizeApps(packageNames: Seq[String]): Free[NineCardsServices, Seq[GooglePlayApp]] = for {
    persistenceApps <- getCategories(packageNames)
    googlePlayApps <- getCategoriesFromGooglePlay(persistenceApps.notFoundApps)
  } yield (persistenceApps.categorizedApps ++ googlePlayApps.categorizedApps) map toGooglePlayApp


  def userbyIdUser(userId : String) : Free[NineCardsServices, User] = for {
    persistenceApps <- getUserByIdUser(userId)
  } yield  (persistenceApps map toUserApp).getOrElse(throw new RuntimeException(""))
}

object AppProcesses {

  implicit def appProcesses(implicit AP: AppPersistenceServices[NineCardsServices], AG: AppGooglePlayServices[NineCardsServices]) = new AppProcesses()
}
