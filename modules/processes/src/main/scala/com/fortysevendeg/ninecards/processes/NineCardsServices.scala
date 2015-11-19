package com.fortysevendeg.ninecards.processes

import com.fortysevendeg.ninecards.services.free.algebra.appsGooglePlay.AppGooglePlayOps
import com.fortysevendeg.ninecards.services.free.algebra.appsPersistence.AppPersistenceOps
import com.fortysevendeg.ninecards.services.free.algebra.sharedCollectionSubscriptions.SharedCollectionSubscriptionOps
import com.fortysevendeg.ninecards.services.free.algebra.sharedCollections.SharedCollectionOps
import com.fortysevendeg.ninecards.services.free.algebra.user.UserOps
import com.fortysevendeg.ninecards.services.free.interpreter.interpreters._

import scalaz.Free._
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

  def point[A](a: => A): FreeC[NineCardsServices, A] = Monad[AFree].point(a)

  type ACoyo[A] = Coyoneda[NineCardsServices, A]

  type AFree[A] = Free[ACoyo, A]

  val coyoint = Coyoneda.liftTF(interpreters)
}
