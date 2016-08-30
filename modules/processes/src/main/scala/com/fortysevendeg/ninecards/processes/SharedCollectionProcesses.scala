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
import com.fortysevendeg.ninecards.services.free.domain.GooglePlay.AppsInfo
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

  def createCollection(request: CreateCollectionRequest): Free[F, CreateOrUpdateCollectionResponse] = {
    for {
      collection ← collectionPersistence.addCollection[SharedCollectionServices](request.collection)
      addedPackages ← collectionPersistence.addPackages(collection.id, request.packages)
    } yield CreateOrUpdateCollectionResponse(
      publicIdentifier = collection.publicIdentifier,
      packagesStats    = PackagesStats(added = addedPackages)
    )
  }.liftF[F]

  def getCollectionByPublicIdentifier(
    publicIdentifier: String,
    authParams: AuthParams
  ): Free[F, XorGetCollectionByPublicId] = {
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

  def getLatestCollectionsByCategory(
    category: String,
    authParams: AuthParams,
    pageNumber: Int,
    pageSize: Int
  ): Free[F, GetCollectionsResponse] =
    getCollections(collectionPersistence.getLatestCollectionsByCategory(category, pageNumber, pageSize), authParams)

  def getPublishedCollections(
    userId: Long,
    authParams: AuthParams
  ): Free[F, GetCollectionsResponse] =
    getCollections(collectionPersistence.getCollectionsByUserId(userId), authParams)

  def getTopCollectionsByCategory(
    category: String,
    authParams: AuthParams,
    pageNumber: Int,
    pageSize: Int
  ): Free[F, GetCollectionsResponse] =
    getCollections(collectionPersistence.getTopCollectionsByCategory(category, pageNumber, pageSize), authParams)

  /**
    * This process changes the application state to one where the user is subscribed to the collection.
    */

  def subscribe(publicIdentifier: String, userId: Long): Free[F, Xor[Throwable, SubscribeResponse]] = {

    def addSubscription(
      subscription: Option[SharedCollectionSubscription],
      collectionId: Long
    ): ConnectionIO[SubscribeResponse] =
      subscription
        .fold(subscriptionPersistence.addSubscription[SharedCollectionSubscription](collectionId, userId))(_.point[ConnectionIO])
        .map(_ ⇒ SubscribeResponse())

    for {
      collection ← findCollection(publicIdentifier)
      subscription ← subscriptionPersistence.getSubscriptionByCollectionAndUser(collection.id, userId).rightXorT[Throwable]
      subscriptionInfo ← addSubscription(subscription, collection.id).rightXorT[Throwable]
    } yield subscriptionInfo
  }.value.liftF[F]

  def unsubscribe(publicIdentifier: String, userId: Long): Free[F, Xor[Throwable, UnsubscribeResponse]] = {
    for {
      collection ← findCollection(publicIdentifier)
      _ ← subscriptionPersistence.removeSubscriptionByCollectionAndUser(collection.id, userId).rightXorT[Throwable]
    } yield UnsubscribeResponse()
  }.value.liftF

  def updateCollection(
    publicIdentifier: String,
    collectionInfo: Option[SharedCollectionUpdateInfo],
    packages: Option[List[String]]
  ): Free[F, Xor[Throwable, CreateOrUpdateCollectionResponse]] = {

    def updateCollectionInfo(collectionId: Long, info: Option[SharedCollectionUpdateInfo]) =
      info
        .map(c ⇒ collectionPersistence.updateCollectionInfo(collectionId, c.title))
        .getOrElse(0.point[ConnectionIO])

    def updatePackages(collectionId: Long, packagesName: Option[List[String]]) =
      packagesName
        .map(p ⇒ collectionPersistence.updatePackages(collectionId, p))
        .getOrElse((0, 0).point[ConnectionIO])

    for {
      collection ← findCollection(publicIdentifier)
      _ ← updateCollectionInfo(collection.id, collectionInfo).rightXorT[Throwable]
      info ← updatePackages(collection.id, packages).rightXorT[Throwable]
      (added, removed) = info
    } yield CreateOrUpdateCollectionResponse(
      collection.publicIdentifier,
      packagesStats = (PackagesStats.apply _).tupled((added, Option(removed)))
    )
  }.value.liftF

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

  private def getCollections(
    sharedCollections: ConnectionIO[List[SharedCollectionServices]],
    authParams: AuthParams
  ) = {

    import scalaz.std.list.listInstance
    import scalaz.syntax.traverse.ToTraverseOps

    def getGooglePlayInfoForPackages(
      collections: List[SharedCollection],
      authParams: AuthParams
    ): Free[F, AppsInfo] = {
      val packages = collections.flatMap(_.packages).toSet.toList
      googlePlayServices.resolveMany(packages, toAuthParamsServices(authParams))
    }

    def fillGooglePlayInfoForPackages(
      collections: List[SharedCollection],
      appsInfo: AppsInfo
    ) = GetCollectionsResponse {
      collections map { collection ⇒
        val foundAppInfo = appsInfo.apps.filter(a ⇒ collection.packages.contains(a.packageName))

        toSharedCollectionWithAppsInfo(collection, foundAppInfo)
      }
    }

    val collectionsWithPackages = sharedCollections flatMap { collections ⇒
      collections.traverse[ConnectionIO, SharedCollection](getCollectionPackages)
    }

    for {
      collections ← collectionsWithPackages.liftF[F]
      appsInfo ← getGooglePlayInfoForPackages(collections, authParams)
    } yield fillGooglePlayInfoForPackages(collections, appsInfo)
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