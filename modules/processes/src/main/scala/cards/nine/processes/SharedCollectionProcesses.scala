package cards.nine.processes

import cards.nine.commons.FreeUtils._
import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService.NineCardsService
import cards.nine.domain.application.{ BasicCard, CardList, Package }
import cards.nine.domain.market.MarketCredentials
import cards.nine.domain.pagination.Page
import cards.nine.processes.ProcessesExceptions.SharedCollectionNotFoundException
import cards.nine.processes.converters.Converters._
import cards.nine.processes.messages.SharedCollectionMessages._
import cards.nine.processes.utils.XorTSyntax._
import cards.nine.services.free.algebra
import cards.nine.services.free.algebra.{ Firebase, GooglePlay }
import cards.nine.services.free.domain.Firebase._
import cards.nine.services.free.domain.{ BaseSharedCollection, SharedCollectionSubscription }
import cats.data.{ Xor, XorT }
import cats.free.Free
import cats.instances.list._
import cats.syntax.traverse._

class SharedCollectionProcesses[F[_]](
  implicit
  collectionServices: algebra.SharedCollection.Services[F],
  notificationsServices: Firebase.Services[F],
  googlePlayServices: GooglePlay.Services[F],
  subscriptionServices: algebra.Subscription.Services[F],
  userServices: algebra.User.Services[F]
) {

  val sharedCollectionNotFoundException = SharedCollectionNotFoundException(
    message = "The required shared collection doesn't exist"
  )

  def createCollection(request: CreateCollectionRequest): Free[F, CreateOrUpdateCollectionResponse] = {
    for {
      collection ← collectionServices.add(request.collection)
      addedPackages ← collectionServices.addPackages(collection.id, request.packages)
    } yield CreateOrUpdateCollectionResponse(
      publicIdentifier = collection.publicIdentifier,
      packagesStats    = PackagesStats(added = addedPackages)
    )
  }

  def getCollectionByPublicIdentifier(
    userId: Long,
    publicIdentifier: String,
    marketAuth: MarketCredentials
  ): Free[F, XorGetCollectionByPublicId] = {
    for {
      sharedCollection ← findCollection(publicIdentifier)
      sharedCollectionInfo ← getCollectionPackages(userId)(sharedCollection).rightXorT[Throwable]
      resolvedSharedCollection ← getAppsInfoForCollection(sharedCollectionInfo, marketAuth)
    } yield resolvedSharedCollection
  }.value

  def getLatestCollectionsByCategory(
    userId: Long,
    category: String,
    marketAuth: MarketCredentials,
    pageParams: Page
  ): Free[F, GetCollectionsResponse] =
    getCollections(collectionServices.getLatestByCategory(category, pageParams), userId, marketAuth)

  def getPublishedCollections(
    userId: Long,
    marketAuth: MarketCredentials
  ): Free[F, GetCollectionsResponse] =
    getCollections(collectionServices.getByUser(userId), userId, marketAuth)

  def getTopCollectionsByCategory(
    userId: Long,
    category: String,
    marketAuth: MarketCredentials,
    pageParams: Page
  ): Free[F, GetCollectionsResponse] =
    getCollections(collectionServices.getTopByCategory(category, pageParams), userId, marketAuth)

  /**
    * This process changes the application state to one where the user is subscribed to the collection.
    */

  def getSubscriptionsByUser(user: Long): Free[F, GetSubscriptionsByUserResponse] =
    subscriptionServices.getByUser(user) map toGetSubscriptionsByUserResponse

  def subscribe(publicIdentifier: String, user: Long): Free[F, Xor[Throwable, SubscribeResponse]] = {

    def addSubscription(
      subscription: Option[SharedCollectionSubscription],
      collectionId: Long,
      collectionPublicId: String
    ) = {
      val subscriptionCount = 1

      subscription
        .fold(subscriptionServices.add(collectionId, user, collectionPublicId))(_ ⇒ subscriptionCount.toFree)
        .map(_ ⇒ SubscribeResponse())
    }

    for {
      collection ← findCollection(publicIdentifier)
      subscription ← subscriptionServices.getByCollectionAndUser(collection.id, user).rightXorT[Throwable]
      subscriptionInfo ← addSubscription(subscription, collection.id, collection.publicIdentifier).rightXorT[Throwable]
    } yield subscriptionInfo
  }.value

  def unsubscribe(publicIdentifier: String, userId: Long): Free[F, Xor[Throwable, UnsubscribeResponse]] = {
    for {
      collection ← findCollection(publicIdentifier)
      _ ← subscriptionServices.removeByCollectionAndUser(collection.id, userId).rightXorT[Throwable]
    } yield UnsubscribeResponse()
  }.value

  def sendNotifications(
    publicIdentifier: String,
    packagesName: List[Package]
  ): NineCardsService[F, SendNotificationResponse] = {

    if (packagesName.isEmpty)
      NineCardsService.right[F, SendNotificationResponse](SendNotificationResponse.emptyResponse)
    else {

      for {
        subscribers ← userServices.getSubscribedInstallationByCollection(publicIdentifier)
        response ← notificationsServices.sendUpdatedCollectionNotification(
          UpdatedCollectionNotificationInfo(
            deviceTokens     = subscribers flatMap (_.deviceToken),
            publicIdentifier = publicIdentifier,
            packagesName     = packagesName
          )
        )
      } yield response
    }
  }

  def updateCollection(
    publicIdentifier: String,
    collectionInfo: Option[SharedCollectionUpdateInfo],
    packages: Option[List[Package]]
  ): Free[F, Xor[Throwable, CreateOrUpdateCollectionResponse]] = {

    def updateCollectionInfo(collectionId: Long, info: Option[SharedCollectionUpdateInfo]) =
      info
        .map(c ⇒ collectionServices.update(collectionId, c.title))
        .getOrElse(0.toFree)

    def updatePackages(collectionId: Long, packagesName: Option[List[Package]]) =
      packagesName
        .map(p ⇒ collectionServices.updatePackages(collectionId, p))
        .getOrElse((List.empty[Package], List.empty[Package]).toFree)

    def updateCollectionAndPackages(
      publicIdentifier: String,
      collectionInfo: Option[SharedCollectionUpdateInfo],
      packages: Option[List[Package]]
    ): Free[F, Throwable Xor (List[Package], List[Package])] = {
      for {
        collection ← findCollection(publicIdentifier)
        _ ← updateCollectionInfo(collection.id, collectionInfo).rightXorT[Throwable]
        info ← updatePackages(collection.id, packages).rightXorT[Throwable]
      } yield info
    }.value

    {
      for {
        updateInfo ← updateCollectionAndPackages(publicIdentifier, collectionInfo, packages).toXorT
        (addedPackages, removedPackages) = updateInfo
        _ ← sendNotifications(publicIdentifier, addedPackages).value.rightXorT[Throwable]
      } yield CreateOrUpdateCollectionResponse(
        publicIdentifier,
        packagesStats = (PackagesStats.apply _).tupled((addedPackages.size, Option(removedPackages.size)))
      )
    }.value
  }

  private[this] def findCollection(publicId: String) =
    collectionServices
      .getByPublicId(publicId)
      .map(Xor.fromOption(_, sharedCollectionNotFoundException))
      .toXorT

  private def getAppsInfoForCollection(
    collection: SharedCollection,
    marketAuth: MarketCredentials
  ): XorT[Free[F, ?], Throwable, GetCollectionByPublicIdentifierResponse] = {
    googlePlayServices.resolveManyDetailed(collection.packages, marketAuth) map { appsInfo ⇒
      GetCollectionByPublicIdentifierResponse(
        toSharedCollectionWithAppsInfo(collection, appsInfo.cards)
      )
    }
  }.rightXorT[Throwable]

  private def getCollections[T <: BaseSharedCollection](
    sharedCollections: Free[F, List[T]],
    userId: Long,
    marketAuth: MarketCredentials
  ) = {

    def getGooglePlayInfoForPackages(
      collections: List[SharedCollection],
      marketAuth: MarketCredentials
    ): Free[F, CardList[BasicCard]] = {
      val packages = collections.flatMap(_.packages).distinct
      googlePlayServices.resolveManyBasic(packages, marketAuth)
    }

    def fillGooglePlayInfoForPackages(
      collections: List[SharedCollection],
      appsInfo: CardList[BasicCard]
    ) = GetCollectionsResponse {
      collections map { collection ⇒
        val foundAppInfo = appsInfo.cards.filter(a ⇒ collection.packages.contains(a.packageName))
        toSharedCollectionWithAppsInfo(collection, foundAppInfo)
      }
    }

    val collectionsWithPackages = sharedCollections flatMap { collections ⇒
      collections.traverse[Free[F, ?], SharedCollection](getCollectionPackages(userId))
    }

    for {
      collections ← collectionsWithPackages
      appsInfo ← getGooglePlayInfoForPackages(collections, marketAuth)
    } yield fillGooglePlayInfoForPackages(collections, appsInfo)
  }

  private[this] def getCollectionPackages(userId: Long)(collection: BaseSharedCollection) =
    collectionServices.getPackagesByCollection(collection.sharedCollectionId) map { packages ⇒
      toSharedCollection(collection, packages map (p ⇒ Package(p.packageName)), userId)
    }
}

object SharedCollectionProcesses {

  implicit def processes[F[_]](
    implicit
    collectionServices: algebra.SharedCollection.Services[F],
    firebaseNotificationsServices: Firebase.Services[F],
    googlePlayServices: GooglePlay.Services[F],
    subscriptionServices: algebra.Subscription.Services[F],
    userServices: algebra.User.Services[F]
  ) = new SharedCollectionProcesses

}