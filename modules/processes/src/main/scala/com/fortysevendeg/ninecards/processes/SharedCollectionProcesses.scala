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
import com.fortysevendeg.ninecards.services.free.algebra.{ Firebase, GooglePlay }
import com.fortysevendeg.ninecards.services.free.domain.Firebase._
import com.fortysevendeg.ninecards.services.free.domain.GooglePlay.AppsInfo
import com.fortysevendeg.ninecards.services.free.domain.{
  BaseSharedCollection,
  Installation,
  SharedCollectionSubscription,
  SharedCollection ⇒ SharedCollectionServices
}
import com.fortysevendeg.ninecards.services.persistence._
import doobie.imports._

import scalaz.concurrent.Task
import scalaz.syntax.applicative._

class SharedCollectionProcesses[F[_]](
  implicit
  collectionPersistence: SharedCollectionPersistenceServices,
  subscriptionPersistence: SharedCollectionSubscriptionPersistenceServices,
  userPersistence: UserPersistenceServices,
  transactor: Transactor[Task],
  dbOps: DBOps[F],
  firebaseNotificationsServices: Firebase.Services[F],
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
  }.liftF

  def getCollectionByPublicIdentifier(
    publicIdentifier: String,
    authParams: AuthParams
  ): Free[F, XorGetCollectionByPublicId] = {
    val unresolvedSharedCollectionInfo: XorT[ConnectionIO, Throwable, SharedCollection] = for {
      sharedCollection ← findCollection(publicIdentifier)
      sharedCollectionInfo ← getCollectionPackages(sharedCollection).rightXorT[Throwable]
    } yield sharedCollectionInfo

    unresolvedSharedCollectionInfo
      .value
      .liftF
      .toXorT
      .flatMap { collection ⇒ getAppsInfoForCollection(collection, authParams) }
      .value
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

  def getSubscriptionsByUser(userId: Long): Free[F, GetSubscriptionsByUserResponse] =
    subscriptionPersistence.getSubscriptionsByUser(userId).liftF map toGetSubscriptionsByUserResponse

  def subscribe(publicIdentifier: String, userId: Long): Free[F, Xor[Throwable, SubscribeResponse]] = {

    def addSubscription(
      subscription: Option[SharedCollectionSubscription],
      collectionId: Long,
      collectionPublicId: String
    ): ConnectionIO[SubscribeResponse] = {
      val subscriptionCount = 1

      subscription
        .fold(subscriptionPersistence.addSubscription(collectionId, userId, collectionPublicId))(_ ⇒ subscriptionCount.point[ConnectionIO])
        .map(_ ⇒ SubscribeResponse())
    }

    for {
      collection ← findCollection(publicIdentifier)
      subscription ← subscriptionPersistence.getSubscriptionByCollectionAndUser(collection.id, userId).rightXorT[Throwable]
      subscriptionInfo ← addSubscription(subscription, collection.id, collection.publicIdentifier).rightXorT[Throwable]
    } yield subscriptionInfo
  }.value.liftF

  def unsubscribe(publicIdentifier: String, userId: Long): Free[F, Xor[Throwable, UnsubscribeResponse]] = {
    for {
      collection ← findCollection(publicIdentifier)
      _ ← subscriptionPersistence.removeSubscriptionByCollectionAndUser(collection.id, userId).rightXorT[Throwable]
    } yield UnsubscribeResponse()
  }.value.liftF

  def sendNotifications(
    publicIdentifier: String,
    packagesName: List[String]
  ): Free[F, List[FirebaseError Xor NotificationResponse]] = {
    import cats.std.list._
    import cats.syntax.traverse._

    def toUpdateCollectionNotificationInfoList(installations: List[Installation]) =
      installations.flatMap(_.deviceToken).grouped(1000).toList

    def sendNotificationsByDeviceTokenGroup(
      publicIdentifier: String,
      packagesName: List[String]
    )(
      deviceTokens: List[String]
    ) =
      firebaseNotificationsServices.sendUpdatedCollectionNotification(
        UpdatedCollectionNotificationInfo(deviceTokens, publicIdentifier, packagesName)
      )

    if (packagesName.isEmpty)
      Free.pure(List.empty[FirebaseError Xor NotificationResponse])
    else
      userPersistence.getSubscribedInstallationByCollection(publicIdentifier).liftF flatMap {
        installations ⇒
          toUpdateCollectionNotificationInfoList(installations)
            .traverse[Free[F, ?], FirebaseError Xor NotificationResponse] {
              sendNotificationsByDeviceTokenGroup(publicIdentifier, packagesName)
            }
      }
  }

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
        .getOrElse((List.empty[String], List.empty[String]).point[ConnectionIO])

    def updateCollectionAndPackages(
      publicIdentifier: String,
      collectionInfo: Option[SharedCollectionUpdateInfo],
      packages: Option[List[String]]
    ): Free[F, Throwable Xor (List[String], List[String])] = {
      for {
        collection ← findCollection(publicIdentifier)
        _ ← updateCollectionInfo(collection.id, collectionInfo).rightXorT[Throwable]
        info ← updatePackages(collection.id, packages).rightXorT[Throwable]
      } yield info
    }.value.liftF

    {
      for {
        updateInfo ← updateCollectionAndPackages(publicIdentifier, collectionInfo, packages).toXorT
        (addedPackages, removedPackages) = updateInfo
        _ ← sendNotifications(publicIdentifier, addedPackages).rightXorT[Throwable]
      } yield CreateOrUpdateCollectionResponse(
        publicIdentifier,
        packagesStats = (PackagesStats.apply _).tupled((addedPackages.size, Option(removedPackages.size)))
      )
    }.value
  }

  private[this] def findCollection(publicId: String) =
    collectionPersistence
      .getCollectionByPublicIdentifier(publicId)
      .map(Xor.fromOption(_, sharedCollectionNotFoundException))
      .toXorT

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

  private def getCollections[T <: BaseSharedCollection](
    sharedCollections: ConnectionIO[List[T]],
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
      collections ← collectionsWithPackages.liftF
      appsInfo ← getGooglePlayInfoForPackages(collections, authParams)
    } yield fillGooglePlayInfoForPackages(collections, appsInfo)
  }

  private[this] def getCollectionPackages(collection: BaseSharedCollection): ConnectionIO[SharedCollection] =
    collectionPersistence.getPackagesByCollection(collection.sharedCollectionId) map { packages ⇒
      toSharedCollection(collection, packages map (_.packageName))
    }
}

object SharedCollectionProcesses {

  implicit def sharedCollectionProcesses[F[_]](
    implicit
    collectionPersistence: SharedCollectionPersistenceServices,
    subscriptionPersistence: SharedCollectionSubscriptionPersistenceServices,
    userPersistence: UserPersistenceServices,
    dbOps: DBOps[F],
    firebaseNotificationsServices: Firebase.Services[F],
    googlePlayServices: GooglePlay.Services[F]
  ) = new SharedCollectionProcesses

}