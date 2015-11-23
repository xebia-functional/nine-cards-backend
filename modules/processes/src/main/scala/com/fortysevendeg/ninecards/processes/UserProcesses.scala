package com.fortysevendeg.ninecards.processes

import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollections.SharedCollectionServices
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollectionSubscriptions.SharedCollectionSubscriptionServices
import com.fortysevendeg.ninecards.services.free.algebra.Users.UserServices

import scala.language.higherKinds

class UserProcesses[F[_]](
  implicit
  U: UserServices[NineCardsServices],
  SC: SharedCollectionServices[NineCardsServices],
  SCS: SharedCollectionSubscriptionServices[NineCardsServices]) {

}
