package com.fortysevendeg.ninecards.processes

import cats.data.Coproduct
import cats.~>
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBResult
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollectionSubscriptions.SharedCollectionSubscriptionOps
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollections.SharedCollectionOps
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

import scalaz.concurrent.Task

object NineCardsServices {

  type ServicesCO1[A] = Coproduct[SharedCollectionOps, SharedCollectionSubscriptionOps, A]
  type NineCardsServices[A] = Coproduct[DBResult, ServicesCO1, A]

  val interpretersCO1: ServicesCO1 ~> Task = SharedCollectionInterpreter or SharedCollectionSubscriptionInterpreter
  val interpreters: NineCardsServices ~> Task = DBResultInterpreter or interpretersCO1

}