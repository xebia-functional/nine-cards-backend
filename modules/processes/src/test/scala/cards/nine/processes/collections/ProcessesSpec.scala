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

import cards.nine.commons.NineCardsErrors.NineCardsError
import cards.nine.commons.NineCardsService._
import cards.nine.processes.NineCardsServices._
import cards.nine.processes.TestInterpreters
import cards.nine.processes.collections.TestData.Messages._
import cards.nine.processes.collections.TestData.Values._
import cards.nine.processes.collections.TestData._
import cards.nine.processes.collections.messages._
import cards.nine.services.free.algebra._
import cats.free.FreeApplicative
import org.specs2.ScalaCheck
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class SharedCollectionProcessesSpec
  extends Specification
  with Matchers
  with Mockito
  with TestInterpreters
  with ScalaCheck {

  trait BasicScope extends Scope {

    implicit val collectionServices: CollectionR[NineCardsServices] =
      mock[CollectionR[NineCardsServices]]
    implicit val subscriptionServices: SubscriptionR[NineCardsServices] =
      mock[SubscriptionR[NineCardsServices]]
    implicit val userServices: UserR[NineCardsServices] =
      mock[UserR[NineCardsServices]]
    implicit val firebaseServices: Firebase[NineCardsServices] =
      mock[Firebase[NineCardsServices]]
    implicit val googlePlayServices: GooglePlay[NineCardsServices] =
      mock[GooglePlay[NineCardsServices]]

    val sharedCollectionProcesses = SharedCollectionProcesses.processes[NineCardsServices]
  }

  def rightFS[A](x: A): FreeApplicative[NineCardsServices, Result[A]] = FreeApplicative.pure(Right(x))
  def leftFS[A](x: NineCardsError): FreeApplicative[NineCardsServices, Result[A]] = FreeApplicative.pure(Left(x))

  "createCollection" should {
    "return a valid response info when the shared collection is created" in new BasicScope {

      collectionServices.add(collection = sharedCollectionDataServices) returns rightFS(collection)

      sharedCollectionProcesses
        .createCollection(createCollectionRequest)
        .foldMap(testInterpreters) must beRight(createCollectionResponse)
    }
  }

  "getCollectionByPublicIdentifier" should {
    "return a valid shared collection info when the shared collection exists" in new BasicScope {

      collectionServices.getByPublicId(publicIdentifier) returns rightFS(collection)
      googlePlayServices.resolveManyDetailed(any, any) returns rightFS(appsInfo)

      sharedCollectionProcesses.getCollectionByPublicIdentifier(
        userId           = publisherId,
        publicIdentifier = publicIdentifier,
        marketAuth       = marketAuth
      ).foldMap(testInterpreters) must beRight(getCollectionByPublicIdentifierResponse)
    }

    "return a SharedCollectionNotFoundException when the shared collection doesn't exist" in
      new BasicScope {

        collectionServices.getByPublicId(publicId = publicIdentifier) returns leftFS(sharedCollectionNotFoundError)

        val collectionInfo = sharedCollectionProcesses.getCollectionByPublicIdentifier(
          userId           = publisherId,
          publicIdentifier = publicIdentifier,
          marketAuth       = marketAuth
        )

        collectionInfo.foldMap(testInterpreters) must beLeft(sharedCollectionNotFoundError)
      }
  }

  "getLatestCollectionsByCategory" should {

    "return a list of Shared collections of the given category" in new BasicScope {

      collectionServices.getLatestByCategory(category, pageParams) returns rightFS(List(collection))
      googlePlayServices.resolveManyBasic(any, any) returns rightFS(appsInfoBasic)

      sharedCollectionProcesses
        .getLatestCollectionsByCategory(
          userId     = publisherId,
          category   = category,
          marketAuth = marketAuth,
          pageParams = pageParams
        )
        .foldMap(testInterpreters) must beRight(GetCollectionsResponse(List(sharedCollectionWithAppsInfoBasic)))
    }

  }

  "getPublishedCollections" should {

    "return a list of shared collections of an user" in new BasicScope {

      collectionServices.getByUser(publisherId) returns rightFS(List(collectionWithSubscriptions))
      googlePlayServices.resolveManyBasic(any, any) returns rightFS(appsInfoBasic)

      sharedCollectionProcesses
        .getPublishedCollections(publisherId, marketAuth)
        .foldMap(testInterpreters) must beRight(GetCollectionsResponse(List(sharedCollectionWithAppsInfoAndSubscriptions)))
    }
  }

  "getSubscriptionsByUser" should {

    "return a list of public identifiers of collections which the user is subscribed to" in new BasicScope {

      subscriptionServices.getByUser(subscriberId) returns rightFS(List(subscription))

      sharedCollectionProcesses
        .getSubscriptionsByUser(subscriberId)
        .foldMap(testInterpreters) must beRight(GetSubscriptionsByUserResponse(List(publicIdentifier)))
    }

  }

  "getTopCollectionsByCategory" should {

    "return a list of Shared collections of the given category" in new BasicScope {
      collectionServices.getTopByCategory(category, pageParams) returns rightFS(List(collection))
      googlePlayServices.resolveManyBasic(any, any) returns rightFS(appsInfoBasic)

      val response = GetCollectionsResponse(List(sharedCollectionWithAppsInfoBasic))

      sharedCollectionProcesses
        .getTopCollectionsByCategory(
          userId     = publisherId,
          category   = category,
          marketAuth = marketAuth,
          pageParams = pageParams
        )
        .foldMap(testInterpreters) must beRight(response)
    }
  }

  "subscribe" should {

    "return a SharedCollectionNotFound error when the shared collection does not exist" in new BasicScope {

      collectionServices.getByPublicId(publicId = publicIdentifier) returns leftFS(sharedCollectionNotFoundError)

      sharedCollectionProcesses
        .subscribe(publicIdentifier, subscriberId)
        .foldMap(testInterpreters) must beLeft(sharedCollectionNotFoundError)
    }

    "return a valid response if the subscription already exists  " in new BasicScope {

      collectionServices.getByPublicId(publicIdentifier) returns rightFS(collection)
      subscriptionServices.getByCollectionAndUser(any, any) returns rightFS(Option(subscription))

      sharedCollectionProcesses
        .subscribe(publicIdentifier, subscriberId)
        .foldMap(testInterpreters) must beRight(SubscribeResponse())
    }

    "return a valid response if it has created a subscription " in new BasicScope {

      collectionServices.getByPublicId(publicIdentifier) returns rightFS(collection)
      subscriptionServices.add(any, any, any) returns rightFS(updatedSubscriptionsCount)
      subscriptionServices.getByCollectionAndUser(any, any) returns rightFS(None)

      sharedCollectionProcesses
        .subscribe(publicIdentifier, subscriberId)
        .foldMap(testInterpreters) must beRight(SubscribeResponse())
    }

  }

  "unsubscribe" should {

    "return a SharedCollectionNotFound error when the shared collection does not exist" in new BasicScope {
      collectionServices.getByPublicId(publicId = publicIdentifier) returns leftFS(sharedCollectionNotFoundError)

      val subscriptionInfo = sharedCollectionProcesses.unsubscribe(publicIdentifier, subscriberId)
      subscriptionInfo.foldMap(testInterpreters) must beLeft(sharedCollectionNotFoundError)
    }

    "return a valid response if the subscription existed" in new BasicScope {

      collectionServices.getByPublicId(publicIdentifier) returns rightFS(collection)
      subscriptionServices.removeByCollectionAndUser(any, any) returns rightFS(1)

      val subscriptionInfo = sharedCollectionProcesses.unsubscribe(publicIdentifier, subscriberId)
      subscriptionInfo.foldMap(testInterpreters) must beRight(UnsubscribeResponse())
    }

    "return a valid response if the subscription did not existed " in new BasicScope {

      collectionServices.getByPublicId(publicIdentifier) returns rightFS(collection)
      subscriptionServices.removeByCollectionAndUser(any, any) returns rightFS(0)

      val subscriptionInfo = sharedCollectionProcesses.unsubscribe(publicIdentifier, subscriberId)
      subscriptionInfo.foldMap(testInterpreters) must beRight(UnsubscribeResponse())
    }

  }

  "increaseViewsCountByOne" should {
    "increase the number of views by 1 if the shared collection exists" in
      new BasicScope {

        collectionServices.getByPublicId(publicIdentifier) returns rightFS(collection)
        collectionServices.increaseViewsByOne(id = collectionId) returns rightFS(updatedCollectionsCount)

        val collectionInfo = sharedCollectionProcesses.increaseViewsCountByOne(publicIdentifier)

        collectionInfo.foldMap(testInterpreters) must beRight[IncreaseViewsCountByOneResponse].which {
          response ⇒ response.publicIdentifier must_== publicIdentifier
        }
      }

    "return a SharedCollectionNotFound error when the shared collection doesn't exist" in
      new BasicScope {

        collectionServices.getByPublicId(publicId = publicIdentifier) returns
          FreeApplicative.pure(Left(sharedCollectionNotFoundError))

        val collectionInfo = sharedCollectionProcesses.increaseViewsCountByOne(publicIdentifier)

        collectionInfo.foldMap(testInterpreters) must beLeft(sharedCollectionNotFoundError)
      }
  }

  "update" should {
    "return the public identifier and the added and removed packages if the shared collection exists" in
      new BasicScope {

        collectionServices.getByPublicId(publicIdentifier) returns rightFS(collection)
        collectionServices.update(collectionId, name) returns rightFS(updatedCollectionsCount)
        collectionServices.updatePackages(collectionId, updatePackagesName) returns rightFS(updatedPackages)
        firebaseServices.sendUpdatedCollectionNotification(any) returns rightFS(sendNotificationResponse)
        userServices.getSubscribedInstallationByCollection(any) returns rightFS(List(installation))

        sharedCollectionProcesses.updateCollection(
          publicIdentifier = publicIdentifier,
          collectionInfo   = Option(sharedCollectionUpdateInfo),
          packages         = Option(updatePackagesName)
        ).foldMap(testInterpreters) must beRight[CreateOrUpdateCollectionResponse].which {
            response ⇒
              response.publicIdentifier must_== publicIdentifier
              response.packagesStats.added must_== addedPackagesCount
              response.packagesStats.removed must beSome(removedPackagesCount)
          }
      }

    "return added and removed packages counts equal to 0 if the package list is not given" in
      new BasicScope {

        collectionServices.getByPublicId(publicIdentifier) returns rightFS(collection)
        collectionServices.update(collectionId, name) returns rightFS(updatedCollectionsCount)

        sharedCollectionProcesses.updateCollection(
          publicIdentifier = publicIdentifier,
          collectionInfo   = Option(sharedCollectionUpdateInfo),
          packages         = None
        ).foldMap(testInterpreters) must beRight[CreateOrUpdateCollectionResponse].which {
            response ⇒
              response.publicIdentifier must_== publicIdentifier
              response.packagesStats.added must_== 0
              response.packagesStats.removed must beSome(0)
          }
      }

    "return a SharedCollectionNotFound error when the shared collection doesn't exist" in
      new BasicScope {

        collectionServices.getByPublicId(publicId = publicIdentifier) returns leftFS(sharedCollectionNotFoundError)

        sharedCollectionProcesses.updateCollection(
          publicIdentifier = publicIdentifier,
          collectionInfo   = Option(sharedCollectionUpdateInfo),
          packages         = Option(updatePackagesName)
        ).foldMap(testInterpreters) must beLeft(sharedCollectionNotFoundError)
      }
  }
}
