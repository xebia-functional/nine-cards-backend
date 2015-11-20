package com.fortysevendeg.ninecards.processes

import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollection.SharedCollectionServices
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollectionSubscription.SharedCollectionSubscriptionServices
import com.fortysevendeg.ninecards.services.free.algebra.User.UserServices

import scala.language.higherKinds

class UserProcesses[F[_]](
  implicit
  U: UserServices[NineCardsServices],
  SC: SharedCollectionServices[NineCardsServices],
  SCS: SharedCollectionSubscriptionServices[NineCardsServices]) {

}
