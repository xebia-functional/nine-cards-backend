package com.fortysevendeg.ninecards

import com.fortysevendeg.ninecards.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.free.algebra.sharedCollectionSubscriptions.SharedCollectionSubscriptionServices
import com.fortysevendeg.ninecards.free.algebra.sharedCollections.SharedCollectionServices
import com.fortysevendeg.ninecards.free.algebra.user.UserServices

import scala.language.higherKinds

class UserProcesses[F[_]](implicit
  U: UserServices[NineCardsServices],
  SC: SharedCollectionServices[NineCardsServices],
  SCS: SharedCollectionSubscriptionServices[NineCardsServices]) {

  import U._
  import SC._
  import SCS._

}
