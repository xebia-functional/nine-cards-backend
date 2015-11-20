package com.fortysevendeg.ninecards.processes

import com.fortysevendeg.ninecards.services.free.algebra.AppGooglePlay.AppGooglePlayOps
import com.fortysevendeg.ninecards.services.free.algebra.AppPersistence.AppPersistenceOps
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollection.SharedCollectionOps
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollectionSubscription.SharedCollectionSubscriptionOps
import com.fortysevendeg.ninecards.services.free.algebra.User.UserOps
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

import scalaz._

object NineCardsServices {

  type NineCardsServices[A] = Coproduct[UserOps, ServicesCO1, A]
  type ServicesCO1[A] = Coproduct[AppGooglePlayOps, ServicesCO2, A]
  type ServicesCO2[A] = Coproduct[AppPersistenceOps, ServicesCO3, A]
  type ServicesCO3[A] = Coproduct[SharedCollectionOps, SharedCollectionSubscriptionOps, A]

  val interpretersCO3: ServicesCO3 ~> Id.Id = or(SharedCollectionInterpreter, SharedCollectionSubscriptionInterpreter)
  val interpretersCO2: ServicesCO2 ~> Id.Id = or(AppPersistenceInterpreter, interpretersCO3)
  val interpretersCO1: ServicesCO1 ~> Id.Id = or(AppGooglePlayInterpreter, interpretersCO2)
  val interpreters: NineCardsServices ~> Id.Id = or(UserInterpreter, interpretersCO1)
}
