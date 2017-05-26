/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.processes.collections

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.application.{ BasicCard, CardList, Package }
import cards.nine.domain.market.MarketCredentials
import cards.nine.domain.pagination.Page
import cards.nine.processes.collections.messages._
import cards.nine.services.free.algebra.{ CollectionR, Firebase, GooglePlay, SubscriptionR, UserR }
import cards.nine.services.free.domain.Firebase._
import cards.nine.services.free.domain.{ BaseSharedCollection, SharedCollectionSubscription }
import freestyle.FreeS

class SharedCollectionProcesses[F[_]](
  implicit
  collectionServices: CollectionR[F],
  notificationsServices: Firebase[F],
  googlePlayServices: GooglePlay[F],
  subscriptionServices: SubscriptionR[F],
  userServices: UserR[F]
) {

  import Converters._

  def toNCS[A](fs: FreeS.Par[F, Result[A]]): NineCardsService[F, A] = NineCardsService[F, A](fs.monad)

  def createCollection(request: CreateCollectionRequest): NineCardsService[F, CreateOrUpdateCollectionResponse] =
    toNCS(collectionServices.add(toSharedCollectionDataServices(request.collection))) map { collection ⇒
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
      sharedCollection ← toNCS(collectionServices.getByPublicId(publicIdentifier))
      collection = toSharedCollection(sharedCollection, userId)
      appsInfo ← toNCS(googlePlayServices.resolveManyDetailed(collection.packages, marketAuth))
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
    toNCS(subscriptionServices.getByUser(user)).map(toGetSubscriptionsByUserResponse)

  def subscribe(publicIdentifier: String, user: Long): NineCardsService[F, SubscribeResponse] = {

    def addSubscription(
      subscription: Option[SharedCollectionSubscription], collectionId: Long
    ): NineCardsService[F, Int] =
      subscription match {
        case None ⇒ toNCS(subscriptionServices.add(collectionId, user, publicIdentifier))
        case Some(_) ⇒ NineCardsService.right(1)
      }

    for {
      collection ← toNCS(collectionServices.getByPublicId(publicIdentifier))
      subscription ← toNCS(subscriptionServices.getByCollectionAndUser(collection.id, user))
      _ ← addSubscription(subscription, collection.id)
    } yield SubscribeResponse()
  }

  def unsubscribe(publicIdentifier: String, userId: Long): NineCardsService[F, UnsubscribeResponse] =
    for {
      collection ← toNCS(collectionServices.getByPublicId(publicIdentifier))
      _ ← toNCS(subscriptionServices.removeByCollectionAndUser(collection.id, userId))
    } yield UnsubscribeResponse()

  def sendNotifications(
    publicIdentifier: String,
    packagesName: List[Package]
  ): NineCardsService[F, SendNotificationResponse] =
    if (packagesName.isEmpty)
      NineCardsService.right[F, SendNotificationResponse](SendNotificationResponse.emptyResponse)
    else {
      for {
        subscribers ← toNCS(userServices.getSubscribedInstallationByCollection(publicIdentifier))
        response ← toNCS(notificationsServices.sendUpdatedCollectionNotification(
          UpdatedCollectionNotificationInfo(
            deviceTokens     = subscribers flatMap (_.deviceToken),
            publicIdentifier = publicIdentifier,
            packagesName     = packagesName
          )
        ))
      } yield response
    }

  def increaseViewsCountByOne(
    publicIdentifier: String
  ): NineCardsService[F, IncreaseViewsCountByOneResponse] =
    for {
      collection ← toNCS(collectionServices.getByPublicId(publicIdentifier))
      _ ← toNCS(collectionServices.increaseViewsByOne(collection.id))
    } yield IncreaseViewsCountByOneResponse(collection.publicIdentifier)

  def updateCollection(
    publicIdentifier: String,
    collectionInfo: Option[SharedCollectionUpdateInfo],
    packages: Option[List[Package]]
  ): NineCardsService[F, CreateOrUpdateCollectionResponse] = {

    def updateCollectionInfo(collectionId: Long, info: Option[SharedCollectionUpdateInfo]) =
      info
        .fold(NineCardsService.right[F, Int](0))(
          updateInfo ⇒ toNCS(collectionServices.update(collectionId, updateInfo.title))
        )

    def updatePackages(collectionId: Long, packagesName: Option[List[Package]]) =
      packagesName
        .fold(NineCardsService.right[F, (List[Package], List[Package])]((Nil, Nil)))(
          packages ⇒ toNCS(collectionServices.updatePackages(collectionId, packages))
        )

    for {
      collection ← toNCS(collectionServices.getByPublicId(publicIdentifier))
      _ ← updateCollectionInfo(collection.id, collectionInfo)
      packagesStats ← updatePackages(collection.id, packages)
      (addedPackages, removedPackages) = packagesStats
      _ ← sendNotifications(publicIdentifier, addedPackages)
    } yield CreateOrUpdateCollectionResponse(
      publicIdentifier,
      packagesStats = PackagesStats(addedPackages.size, Option(removedPackages.size))
    )
  }

  private def getCollections[T <: BaseSharedCollection](
    sharedCollections: FreeS.Par[F, Result[List[T]]],
    userId: Long,
    marketAuth: MarketCredentials
  ): NineCardsService[F, GetCollectionsResponse] = {

    def fillGooglePlayInfoForPackages(appsInfo: CardList[BasicCard])(collection: SharedCollection) = {
      val foundAppInfo = appsInfo.cards.filter(a ⇒ collection.packages.contains(a.packageName))
      toSharedCollectionWithAppsInfo(collection, foundAppInfo)
    }

    for {
      collections ← toNCS(sharedCollections) map toSharedCollectionList(userId)
      packages = collections.flatMap(_.packages).distinct
      appsInfo ← toNCS(googlePlayServices.resolveManyBasic(packages, marketAuth))
    } yield GetCollectionsResponse(
      collections.map(fillGooglePlayInfoForPackages(appsInfo))
    )
  }
}

object SharedCollectionProcesses {

  implicit def processes[F[_]](
    implicit
    collectionServices: CollectionR[F],
    notificationsServices: Firebase[F],
    googlePlayServices: GooglePlay[F],
    subscriptionServices: SubscriptionR[F],
    userServices: UserR[F]
  ) = new SharedCollectionProcesses

}
