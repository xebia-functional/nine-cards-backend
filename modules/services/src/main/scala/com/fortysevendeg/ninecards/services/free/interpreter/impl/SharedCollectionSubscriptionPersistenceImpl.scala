package com.fortysevendeg.ninecards.services.free.interpreter.impl

import com.fortysevendeg.ninecards.services.free.domain.SharedCollectionSubscription

class SharedCollectionSubscriptionPersistenceImpl {

  def addSharedCollectionSubscription(sharedCollectionId: String): SharedCollectionSubscription =
    SharedCollectionSubscription(
      sharedCollectionId = sharedCollectionId,
      userId = "userId")

  def deleteSharedCollectionSubscription(sharedCollectionId: String): Unit = ()
}

object SharedCollectionSubscriptionPersistenceImpl {

  implicit def sharedCollectionSubscriptionPersistenceImpl =
    new SharedCollectionSubscriptionPersistenceImpl()
}
