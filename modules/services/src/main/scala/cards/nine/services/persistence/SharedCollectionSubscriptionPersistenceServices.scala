package cards.nine.services.persistence

import cards.nine.services.free.domain.SharedCollectionSubscription
import doobie.imports._

class SharedCollectionSubscriptionPersistenceServices(
  implicit
  subscriptionPersistence: Persistence[SharedCollectionSubscription]
) {

  import SharedCollectionSubscription.Queries._

  def addSubscription(collectionId: Long, userId: Long, collectionPublicId: String): ConnectionIO[Int] =
    subscriptionPersistence.update(
      sql    = insert,
      values = (collectionId, userId, collectionPublicId)
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