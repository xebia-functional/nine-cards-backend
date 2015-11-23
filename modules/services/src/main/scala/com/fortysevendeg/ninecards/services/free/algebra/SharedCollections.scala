package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.algebra.Utils._
import com.fortysevendeg.ninecards.services.free.domain.{SharedCollection, SharedCollectionSubscription}

import scala.language.higherKinds

object SharedCollections {

  sealed trait SharedCollectionOps[A]

  case class AddSharedCollection(collection: SharedCollection) extends SharedCollectionOps[SharedCollection]

  case class GetSharedCollectionById(sharedCollectionId: String) extends SharedCollectionOps[Option[SharedCollection]]

  case class UpdateInstallNotification(sharedCollectionId: String) extends SharedCollectionOps[SharedCollection]

  case class UpdateViewNotification(sharedCollectionId: String) extends SharedCollectionOps[SharedCollection]

  class SharedCollectionServices[F[_]](implicit I: Inject[SharedCollectionOps, F]) {

    def addSharedCollection(collection: SharedCollection): Free[F, SharedCollection] = lift[SharedCollectionOps, F, SharedCollection](AddSharedCollection(collection))

    def getSharedCollectionById(sharedCollectionId: String): Free[F, Option[SharedCollection]] = lift[SharedCollectionOps, F, Option[SharedCollection]](GetSharedCollectionById(sharedCollectionId))

    def updateInstallNotification(sharedCollectionId: String): Free[F, SharedCollection] = lift[SharedCollectionOps, F, SharedCollection](UpdateInstallNotification(sharedCollectionId))

    def updateViewNotification(sharedCollectionId: String): Free[F, SharedCollection] = lift[SharedCollectionOps, F, SharedCollection](UpdateViewNotification(sharedCollectionId))
  }

  object SharedCollectionServices {

    implicit def dataSource[F[_]](implicit I: Inject[SharedCollectionOps, F]): SharedCollectionServices[F] = new SharedCollectionServices[F]

  }

}

object SharedCollectionSubscriptions {

  sealed trait SharedCollectionSubscriptionOps[A]

  case class AddSharedCollectionSubscription(sharedCollectionId: String) extends SharedCollectionSubscriptionOps[SharedCollectionSubscription]

  case class DeleteSharedCollectionSubscription(sharedCollectionId: String) extends SharedCollectionSubscriptionOps[Unit]

  class SharedCollectionSubscriptionServices[F[_]](implicit I: Inject[SharedCollectionSubscriptionOps, F]) {

    def addSharedCollectionSubscription(sharedCollectionId: String): Free[F, SharedCollectionSubscription] = lift[SharedCollectionSubscriptionOps, F, SharedCollectionSubscription](AddSharedCollectionSubscription(sharedCollectionId))

    def deleteSharedCollectionSubscription(sharedCollectionId: String): Free[F, Unit] = lift[SharedCollectionSubscriptionOps, F, Unit](DeleteSharedCollectionSubscription(sharedCollectionId))
  }

  object SharedCollectionSubscriptionServices {

    implicit def dataSource[F[_]](implicit I: Inject[SharedCollectionSubscriptionOps, F]): SharedCollectionSubscriptionServices[F] = new SharedCollectionSubscriptionServices[F]

  }

}
