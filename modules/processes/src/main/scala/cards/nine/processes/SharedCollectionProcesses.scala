package cards.nine.processes

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService.NineCardsService
import cards.nine.domain.application.{ BasicCard, CardList, Package }
import cards.nine.domain.market.MarketCredentials
import cards.nine.domain.pagination.Page
import cards.nine.processes.converters.Converters._
import cards.nine.processes.messages.SharedCollectionMessages._
import cards.nine.services.free.algebra
import cards.nine.services.free.algebra.{ Firebase, GooglePlay }
import cards.nine.services.free.domain.Firebase._
import cards.nine.services.free.domain.{ BaseSharedCollection, SharedCollectionSubscription }

class SharedCollectionProcesses[F[_]](
  implicit
  collectionServices: algebra.SharedCollection.Services[F],
  notificationsServices: Firebase.Services[F],
  googlePlayServices: GooglePlay.Services[F],
  subscriptionServices: algebra.Subscription.Services[F],
  userServices: algebra.User.Services[F]
) {

  def createCollection(request: CreateCollectionRequest): NineCardsService[F, CreateOrUpdateCollectionResponse] =
    collectionServices.add(request.collection) map { collection ⇒
      CreateOrUpdateCollectionResponse(
        publicIdentifier = collection.publicIdentifier,
        packagesStats    = PackagesStats(added = collection.packages.size)
      )
    }

  def getCollectionByPublicIdentifier(
    userId: Long,
    publicIdentifier: String,
    marketAuth: MarketCredentials
  ): NineCardsService[F, GetCollectionByPublicIdentifierResponse] =
    for {
      sharedCollection ← collectionServices.getByPublicId(publicIdentifier)
      collection = toSharedCollection(sharedCollection, userId)
      appsInfo ← googlePlayServices.resolveManyDetailed(collection.packages, marketAuth)
    } yield GetCollectionByPublicIdentifierResponse(
      toSharedCollectionWithAppsInfo(collection, appsInfo.cards)
    )

  def getLatestCollectionsByCategory(
    userId: Long,
    category: String,
    marketAuth: MarketCredentials,
    pageParams: Page
  ): NineCardsService[F, GetCollectionsResponse] =
    getCollections(collectionServices.getLatestByCategory(category, pageParams), userId, marketAuth)

  def getPublishedCollections(
    userId: Long,
    marketAuth: MarketCredentials
  ): NineCardsService[F, GetCollectionsResponse] =
    getCollections(collectionServices.getByUser(userId), userId, marketAuth)

  def getTopCollectionsByCategory(
    userId: Long,
    category: String,
    marketAuth: MarketCredentials,
    pageParams: Page
  ): NineCardsService[F, GetCollectionsResponse] =
    getCollections(collectionServices.getTopByCategory(category, pageParams), userId, marketAuth)

  def getSubscriptionsByUser(user: Long): NineCardsService[F, GetSubscriptionsByUserResponse] =
    subscriptionServices.getByUser(user) map toGetSubscriptionsByUserResponse

  def subscribe(publicIdentifier: String, user: Long): NineCardsService[F, SubscribeResponse] = {

    def addSubscription(subscription: Option[SharedCollectionSubscription], collectionId: Long) = {
      val subscriptionCount = 1

      subscription
        .fold(subscriptionServices.add(collectionId, user, publicIdentifier))(_ ⇒ NineCardsService.right(subscriptionCount))
        .map(_ ⇒ SubscribeResponse())
    }

    for {
      collection ← collectionServices.getByPublicId(publicIdentifier)
      subscription ← subscriptionServices.getByCollectionAndUser(collection.id, user)
      subscriptionInfo ← addSubscription(subscription, collection.id)
    } yield subscriptionInfo
  }

  def unsubscribe(publicIdentifier: String, userId: Long): NineCardsService[F, UnsubscribeResponse] =
    for {
      collection ← collectionServices.getByPublicId(publicIdentifier)
      _ ← subscriptionServices.removeByCollectionAndUser(collection.id, userId)
    } yield UnsubscribeResponse()

  def sendNotifications(
    publicIdentifier: String,
    packagesName: List[Package]
  ): NineCardsService[F, SendNotificationResponse] =
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

  def updateCollection(
    publicIdentifier: String,
    collectionInfo: Option[SharedCollectionUpdateInfo],
    packages: Option[List[Package]]
  ): NineCardsService[F, CreateOrUpdateCollectionResponse] = {

    def updateCollectionInfo(collectionId: Long, info: Option[SharedCollectionUpdateInfo]) =
      info
        .map(c ⇒ collectionServices.update(collectionId, c.title))
        .getOrElse(NineCardsService.right(0))

    def updatePackages(collectionId: Long, packagesName: Option[List[Package]]) =
      packagesName
        .map(p ⇒ collectionServices.updatePackages(collectionId, p))
        .getOrElse(NineCardsService.right((List.empty[Package], List.empty[Package])))

    for {
      collection ← collectionServices.getByPublicId(publicIdentifier)
      _ ← updateCollectionInfo(collection.id, collectionInfo)
      packagesStats ← updatePackages(collection.id, packages)
      (addedPackages, removedPackages) = packagesStats
      _ ← sendNotifications(publicIdentifier, addedPackages)
    } yield CreateOrUpdateCollectionResponse(
      publicIdentifier,
      packagesStats = (PackagesStats.apply _).tupled((addedPackages.size, Option(removedPackages.size)))
    )
  }

  private def getCollections[T <: BaseSharedCollection](
    sharedCollections: NineCardsService[F, List[T]],
    userId: Long,
    marketAuth: MarketCredentials
  ) = {

    def fillGooglePlayInfoForPackages(appsInfo: CardList[BasicCard])(collection: SharedCollection) = {
      val foundAppInfo = appsInfo.cards.filter(a ⇒ collection.packages.contains(a.packageName))
      toSharedCollectionWithAppsInfo(collection, foundAppInfo)
    }

    for {
      collections ← sharedCollections map toSharedCollectionList(userId)
      packages = collections.flatMap(_.packages).distinct
      appsInfo ← googlePlayServices.resolveManyBasic(packages, marketAuth)
    } yield GetCollectionsResponse(
      collections map fillGooglePlayInfoForPackages(appsInfo)
    )
  }
}

object SharedCollectionProcesses {

  implicit def processes[F[_]](
    implicit
    collectionServices: algebra.SharedCollection.Services[F],
    notificationsServices: Firebase.Services[F],
    googlePlayServices: GooglePlay.Services[F],
    subscriptionServices: algebra.Subscription.Services[F],
    userServices: algebra.User.Services[F]
  ) = new SharedCollectionProcesses

}