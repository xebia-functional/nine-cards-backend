package com.fortysevendeg.ninecards.processes

import cats.data.Coproduct
import cats.~>
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBResult
import com.fortysevendeg.ninecards.services.free.algebra.GoogleApiServices.GoogleApiOps
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollectionSubscriptions.SharedCollectionSubscriptionOps
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollections.SharedCollectionOps
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

import scalaz.concurrent.Task

object NineCardsServices {

  type ServicesCO2[A] = Coproduct[SharedCollectionOps, SharedCollectionSubscriptionOps, A]
  type ServicesCO1[A] = Coproduct[GoogleApiOps, ServicesCO2, A]
  type NineCardsServices[A] = Coproduct[DBResult, ServicesCO1, A]

  val interpretersCO2: ServicesCO2 ~> Task = SharedCollectionInterpreter or SharedCollectionSubscriptionInterpreter
  val interpretersCO1: ServicesCO1 ~> Task = GoogleAPIServicesInterpreter or interpretersCO2
  val interpreters: NineCardsServices ~> Task = DBResultInterpreter or interpretersCO1

}