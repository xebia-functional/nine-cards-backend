package cards.nine.services.free.interpreter.subscription

import cards.nine.services.free.algebra.Subscription._
import cards.nine.services.free.domain.SharedCollectionSubscription
import cards.nine.services.free.domain.SharedCollectionSubscription.Queries
import cards.nine.services.persistence.Persistence
import cats.~>
import doobie.imports._

class Services(persistence: Persistence[SharedCollectionSubscription]) extends (Ops ~> ConnectionIO) {

  def add(collectionId: Long, userId: Long, collectionPublicId: String): ConnectionIO[Int] =
    persistence.update(
      sql    = Queries.insert,
      values = (collectionId, userId, collectionPublicId)
    )

  def getByCollection(collectionId: Long): ConnectionIO[List[SharedCollectionSubscription]] =
    persistence.fetchList(
      sql    = Queries.getByCollection,
      values = collectionId
    )

  def getByCollectionAndUser(collectionId: Long, userId: Long): ConnectionIO[Option[SharedCollectionSubscription]] =
    persistence.fetchOption(
      sql    = Queries.getByCollectionAndUser,
      values = (collectionId, userId)
    )

  def getByUser(userId: Long): ConnectionIO[List[SharedCollectionSubscription]] =
    persistence.fetchList(
      sql    = Queries.getByUser,
      values = userId
    )

  def removeByCollectionAndUser(collectionId: Long, userId: Long): ConnectionIO[Int] =
    persistence.update(
      sql    = Queries.deleteByCollectionAndUser,
      values = (collectionId, userId)
    )

  def apply[A](fa: Ops[A]): ConnectionIO[A] = fa match {
    case Add(collection, user, collectionPublicId) ⇒
      add(collection, user, collectionPublicId)
    case GetByCollection(collection) ⇒
      getByCollection(collection)
    case GetByCollectionAndUser(collection, user) ⇒
      getByCollectionAndUser(collection, user)
    case GetByUser(user) ⇒
      getByUser(user)
    case RemoveByCollectionAndUser(collection, user) ⇒
      removeByCollectionAndUser(collection, user)
  }
}

object Services {

  def services(implicit persistence: Persistence[SharedCollectionSubscription]) =
    new Services(persistence)
}
