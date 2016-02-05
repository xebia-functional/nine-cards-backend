package com.fortysevendeg.ninecards.processes

import cats.data.Coproduct
import cats.~>
import com.fortysevendeg.ninecards.services.free.algebra.AppGooglePlay.AppGooglePlayOps
import com.fortysevendeg.ninecards.services.free.algebra.AppPersistence.AppPersistenceOps
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBResult
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollectionSubscriptions.SharedCollectionSubscriptionOps
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollections.SharedCollectionOps
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

import scalaz.concurrent.Task

object NineCardsServices {

  type ServicesCO3[A] = Coproduct[SharedCollectionOps, SharedCollectionSubscriptionOps, A]
  type ServicesCO2[A] = Coproduct[AppPersistenceOps, ServicesCO3, A]
  type ServicesCO1[A] = Coproduct[AppGooglePlayOps, ServicesCO2, A]
  type NineCardsServices[A] = Coproduct[DBResult, ServicesCO1, A]

  val interpretersCO3: ServicesCO3 ~> Task = SharedCollectionInterpreter or SharedCollectionSubscriptionInterpreter
  val interpretersCO2: ServicesCO2 ~> Task = AppPersistenceInterpreter or interpretersCO3
  val interpretersCO1: ServicesCO1 ~> Task = AppGooglePlayInterpreter or interpretersCO2
  val interpreters: NineCardsServices ~> Task = DBResultInterpreter or interpretersCO1

}