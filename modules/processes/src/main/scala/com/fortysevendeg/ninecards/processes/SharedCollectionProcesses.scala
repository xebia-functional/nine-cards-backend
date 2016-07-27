package com.fortysevendeg.ninecards.processes

import cats.data.{ Xor, XorT }
import cats.free.Free
import com.fortysevendeg.ninecards.processes.ProcessesExceptions.SharedCollectionNotFoundException
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.messages.ApplicationMessages.AuthParams
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.utils.XorTSyntax._
import com.fortysevendeg.ninecards.processes.utils.MonadInstances._
import com.fortysevendeg.ninecards.services.common.ConnectionIOOps._
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBOps
import com.fortysevendeg.ninecards.services.free.algebra.GooglePlay
import com.fortysevendeg.ninecards.services.free.domain.{
  SharedCollection ⇒ SharedCollectionServices,
  SharedCollectionSubscription
}
import com.fortysevendeg.ninecards.services.persistence._
import doobie.imports._

import scalaz.concurrent.Task
import scalaz.syntax.applicative._

class SharedCollectionProcesses[F[_]](
  implicit
  collectionPersistence: SharedCollectionPersistenceServices,
  subscriptionPersistence: SharedCollectionSubscriptionPersistenceServices,
  transactor: Transactor[Task],
  dbOps: DBOps[F],
  googlePlayServices: GooglePlay.Services[F]
) {

  val sharedCollectionNotFoundException = SharedCollectionNotFoundException(
    message = "The required shared collection doesn't exist"
  )

  def createCollection(request: CreateCollectionRequest): Free[F, CreateCollectionResponse] = {
    for {
      collection ← collectionPersistence.addCollection[SharedCollectionServices](request.collection)
      response ← collectionPersistence.addPackages(collection.id, request.packages)
    } yield toCreateCollectionResponse(collection, request.packages)
  }.liftF[F]

  def getCollectionByPublicIdentifier(
    publicIdentifier: String,
    authParams: AuthParams): Free[F, XorGetCollectionByPublicId] = {
    val unresolvedSharedCollectionInfo = for {
      sharedCollection ← findCollection(publicIdentifier)
      sharedCollectionInfo ← getCollectionPackages(sharedCollection).rightXorT[Throwable]
    } yield sharedCollectionInfo

    {
      XorT[Free[F, ?], Throwable, SharedCollection] {
        unresolvedSharedCollectionInfo.value.liftF
      } flatMap { collection ⇒
        getAppsInfoForCollection(collection, authParams)
      }
    }.value
  }

  def getPublishedCollections(
    userId: Long,
    authParams: AuthParams): Free[F, GetPublishedCollectionsResponse] = {
    import scalaz.std.list.listInstance
    import scalaz.syntax.traverse.ToTraverseOps

    val publishedCollections = for {
      collections ← collectionPersistence.getCollectionsByUserId(userId)
      info ← collections.traverse[ConnectionIO, SharedCollection](getCollectionPackages)
    } yield info

    for {
      collections ← publishedCollections.liftF[F]
      collectionWithAppsInfo ← getAppsInfoForCollections(collections, authParams)
    } yield GetPublishedCollectionsResponse(collectionWithAppsInfo)
  }

  /**
    * This process changes the application state to one where the user is subscribed to the collection.
    */

  def subscribe(publicIdentifier: String, userId: Long): Free[F, Xor[Throwable, SubscribeResponse]] = {
    for {
      collection ← findCollection(publicIdentifier)
      subscription ← subscriptionPersistence.getSubscriptionByCollectionAndUser(collection.id, userId).rightXorT[Throwable]
      subscriptionInfo ← addSubscription(subscription, collection.id, userId).rightXorT[Throwable]
    } yield subscriptionInfo
  }.value.liftF[F]

  def unsubscribe(publicIdentifier: String, userId: Long): Free[F, Xor[Throwable, UnsubscribeResponse]] = {
    for {
      collection ← findCollection(publicIdentifier)
      _ ← subscriptionPersistence.removeSubscriptionByCollectionAndUser(collection.id, userId).rightXorT[Throwable]
    } yield UnsubscribeResponse()
  }.value.liftF

  private[this] def addSubscription(
    subscription: Option[SharedCollectionSubscription],
    collectionId: Long,
    userId: Long
  ): ConnectionIO[SubscribeResponse] =
    subscription
      .fold(subscriptionPersistence.addSubscription[SharedCollectionSubscription](collectionId, userId))(_.point[ConnectionIO])
      .map(_ ⇒ SubscribeResponse())

  private[this] def findCollection(publicId: String): XorT[ConnectionIO, Throwable, SharedCollectionServices] =
    XorT[ConnectionIO, Throwable, SharedCollectionServices] {
      collectionPersistence
        .getCollectionByPublicIdentifier(publicId)
        .map(Xor.fromOption(_, sharedCollectionNotFoundException))
    }

  private def getAppsInfoForCollection(
    collection: SharedCollection,
    authParams: AuthParams
  ): XorT[Free[F, ?], Throwable, GetCollectionByPublicIdentifierResponse] = {
    googlePlayServices.resolveMany(collection.packages, toAuthParamsServices(authParams)) map { appsInfo ⇒
      GetCollectionByPublicIdentifierResponse(
        toSharedCollectionWithAppsInfo(collection, appsInfo.apps)
      )
    }
  }.rightXorT[Throwable]

  private def getAppsInfoForCollections(
    collections: List[SharedCollection],
    authParams: AuthParams
  ): Free[F, List[SharedCollectionWithAppsInfo]] = {
    import cats.std.list._
    import cats.syntax.traverse._

    collections.traverse[Free[F, ?], SharedCollectionWithAppsInfo] { collection ⇒
      googlePlayServices.resolveMany(collection.packages, toAuthParamsServices(authParams)) map {
        appsInfo ⇒
          toSharedCollectionWithAppsInfo(collection, appsInfo.apps)
      }
    }
  }

  private[this] def getCollectionPackages(collection: SharedCollectionServices): ConnectionIO[SharedCollection] =
    collectionPersistence.getPackagesByCollection(collection.id) map { packages ⇒
      toSharedCollection(collection, packages map (_.packageName))
    }

}

object SharedCollectionProcesses {

  implicit def sharedCollectionProcesses[F[_]](
    implicit
    sharedCollectionPersistenceServices: SharedCollectionPersistenceServices,
    dbOps: DBOps[F],
    googlePlayServices: GooglePlay.Services[F]
  ) = new SharedCollectionProcesses

}