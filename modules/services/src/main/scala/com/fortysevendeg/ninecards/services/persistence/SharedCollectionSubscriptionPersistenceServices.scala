package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.SharedCollectionSubscription
import doobie.imports._

class SharedCollectionSubscriptionPersistenceServices(
  implicit
  subscriptionPersistence: Persistence[SharedCollectionSubscription]
) {

  import SharedCollectionSubscription.Queries._

  def addSubscription[K](collectionId: Long, userId: Long)(implicit ev: Composite[K]): ConnectionIO[K] =
    subscriptionPersistence.updateWithGeneratedKeys[K](
      sql    = insert,
      fields = SharedCollectionSubscription.allFields,
      values = (collectionId, userId)
    )

  def getSubscriptionsByCollection(collectionId: Long): ConnectionIO[List[SharedCollectionSubscription]] =
    subscriptionPersistence.fetchList(
      sql    = getByCollection,
      values = collectionId
    )

  def getSubscriptionByCollectionAndUser(collectionId: Long, userId: Long): ConnectionIO[Option[SharedCollectionSubscription]] =
    subscriptionPersistence.fetchOption(
      sql    = getByCollectionAndUser,
      values = (collectionId, userId)
    )

  def getSubscriptionById(subscriptionId: Long): ConnectionIO[Option[SharedCollectionSubscription]] =
    subscriptionPersistence.fetchOption(
      sql    = getById,
      values = subscriptionId
    )

  def getSubscriptionsByUser(userId: Long): ConnectionIO[List[SharedCollectionSubscription]] =
    subscriptionPersistence.fetchList(
      sql    = getByUser,
      values = userId
    )

  def removeSubscriptionByCollectionAndUser(collectionId: Long, userId: Long): ConnectionIO[Int] =
    subscriptionPersistence.update(
      sql    = deleteByCollectionAndUser,
      values = (collectionId, userId)
    )
}

object SharedCollectionSubscriptionPersistenceServices {
  implicit def sharedCollectionSubscriptionPersistenceServices(
    implicit
    subscriptionPersistence: Persistence[SharedCollectionSubscription]
  ) =
    new SharedCollectionSubscriptionPersistenceServices
}
