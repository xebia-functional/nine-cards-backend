package com.fortysevendeg.ninecards.processes

import cats.data.Coproduct
import cats.~>
import com.fortysevendeg.ninecards.services.free.algebra.AppGooglePlay.AppGooglePlayOps
import com.fortysevendeg.ninecards.services.free.algebra.AppPersistence.AppPersistenceOps
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBResult
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollections.SharedCollectionOps
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollectionSubscriptions.SharedCollectionSubscriptionOps
import com.fortysevendeg.ninecards.services.free.algebra.Users.UserOps
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

import scalaz.concurrent.Task

object NineCardsServices {

  type ServicesCO4[A] = Coproduct[SharedCollectionOps, SharedCollectionSubscriptionOps, A]
  type ServicesCO3[A] = Coproduct[AppPersistenceOps, ServicesCO4, A]
  type ServicesCO2[A] = Coproduct[AppGooglePlayOps, ServicesCO3, A]
  type ServicesCO1[A] = Coproduct[DBResult, ServicesCO2, A]
  type NineCardsServices[A] = Coproduct[UserOps, ServicesCO1, A]

  val interpretersCO4: ServicesCO4 ~> Task = SharedCollectionInterpreter or SharedCollectionSubscriptionInterpreter
  val interpretersCO3: ServicesCO3 ~> Task = AppPersistenceInterpreter or interpretersCO4
  val interpretersCO2: ServicesCO2 ~> Task = AppGooglePlayInterpreter or interpretersCO3
  val interpretersCO1: ServicesCO1 ~> Task = DBResultInterpreter or interpretersCO2
  val interpreters: NineCardsServices ~> Task = UserInterpreter or interpretersCO1

}