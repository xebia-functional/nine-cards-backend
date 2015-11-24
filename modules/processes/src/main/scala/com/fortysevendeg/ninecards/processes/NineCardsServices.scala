package com.fortysevendeg.ninecards.processes

import cats.arrow.NaturalTransformation._
import cats.data.Coproduct
import cats.~>
import com.fortysevendeg.ninecards.services.free.algebra.AppGooglePlay.AppGooglePlayOps
import com.fortysevendeg.ninecards.services.free.algebra.AppPersistence.AppPersistenceOps
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollections.SharedCollectionOps
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollectionSubscriptions.SharedCollectionSubscriptionOps
import com.fortysevendeg.ninecards.services.free.algebra.Users.UserOps
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

import scalaz.concurrent.Task

object NineCardsServices {

  type ServicesCO3[A] = Coproduct[SharedCollectionOps, SharedCollectionSubscriptionOps, A]
  type ServicesCO2[A] = Coproduct[AppPersistenceOps, ServicesCO3, A]
  type ServicesCO1[A] = Coproduct[AppGooglePlayOps, ServicesCO2, A]
  type NineCardsServices[A] = Coproduct[UserOps, ServicesCO1, A]

  val interpretersCO3: ServicesCO3 ~> Task = or(SharedCollectionInterpreter, SharedCollectionSubscriptionInterpreter)
  val interpretersCO2: ServicesCO2 ~> Task = or(AppPersistenceInterpreter, interpretersCO3)
  val interpretersCO1: ServicesCO1 ~> Task = or(AppGooglePlayInterpreter, interpretersCO2)
  val interpreters: NineCardsServices ~> Task = or(UserInterpreter, interpretersCO1)
}
