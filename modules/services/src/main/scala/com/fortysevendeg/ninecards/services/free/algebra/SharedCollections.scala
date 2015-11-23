package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.domain.{SharedCollection, SharedCollectionSubscription}

import scala.language.higherKinds

object SharedCollections {

  sealed trait SharedCollectionOps[A]

  case class AddSharedCollection(collection: SharedCollection) extends SharedCollectionOps[SharedCollection]

  case class GetSharedCollectionById(sharedCollectionId: String) extends SharedCollectionOps[Option[SharedCollection]]

  case class UpdateInstallNotification(sharedCollectionId: String) extends SharedCollectionOps[SharedCollection]

  case class UpdateViewNotification(sharedCollectionId: String) extends SharedCollectionOps[SharedCollection]

  class SharedCollectionServices[F[_]](implicit I: Inject[SharedCollectionOps, F]) {

    def addSharedCollection(collection: SharedCollection): Free[F, SharedCollection] =
      Free.inject[SharedCollectionOps, F](AddSharedCollection(collection))

    def getSharedCollectionById(sharedCollectionId: String): Free[F, Option[SharedCollection]] =
      Free.inject[SharedCollectionOps, F](GetSharedCollectionById(sharedCollectionId))

    def updateInstallNotification(sharedCollectionId: String): Free[F, SharedCollection] =
      Free.inject[SharedCollectionOps, F](UpdateInstallNotification(sharedCollectionId))

    def updateViewNotification(sharedCollectionId: String): Free[F, SharedCollection] =
      Free.inject[SharedCollectionOps, F](UpdateViewNotification(sharedCollectionId))
  }

  object SharedCollectionServices {

    implicit def dataSource[F[_]](
      implicit I: Inject[SharedCollectionOps, F]): SharedCollectionServices[F] =
      new SharedCollectionServices[F]

  }

}

object SharedCollectionSubscriptions {

  sealed trait SharedCollectionSubscriptionOps[A]

  case class AddSharedCollectionSubscription(sharedCollectionId: String) extends SharedCollectionSubscriptionOps[SharedCollectionSubscription]

  case class DeleteSharedCollectionSubscription(sharedCollectionId: String) extends SharedCollectionSubscriptionOps[Unit]

  class SharedCollectionSubscriptionServices[F[_]](implicit I: Inject[SharedCollectionSubscriptionOps, F]) {

    def addSharedCollectionSubscription(sharedCollectionId: String): Free[F, SharedCollectionSubscription] =
      Free.inject[SharedCollectionSubscriptionOps, F](AddSharedCollectionSubscription(sharedCollectionId))

    def deleteSharedCollectionSubscription(sharedCollectionId: String): Free[F, Unit] =
      Free.inject[SharedCollectionSubscriptionOps, F](DeleteSharedCollectionSubscription(sharedCollectionId))
  }

  object SharedCollectionSubscriptionServices {

    implicit def dataSource[F[_]](
      implicit I: Inject[SharedCollectionSubscriptionOps, F]): SharedCollectionSubscriptionServices[F] =
      new SharedCollectionSubscriptionServices[F]

  }

}
