package cards.nine.processes

import cats.data.Xor
import cats.free.Free
import cards.nine.processes.NineCardsServices._
import cards.nine.processes.TestData.Exceptions._
import cards.nine.processes.TestData.Messages._
import cards.nine.processes.TestData.Values._
import cards.nine.processes.TestData._
import cards.nine.processes.messages.SharedCollectionMessages._
import cards.nine.services.free.algebra
import cards.nine.services.free.algebra.{ Firebase, GooglePlay }
import cards.nine.services.free.domain._
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.ScalaCheck
import org.specs2.matcher.{ Matchers, XorMatchers }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait SharedCollectionProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with XorMatchers
  with TestInterpreters {

  trait BasicScope extends Scope {

    implicit val collectionServices = mock[algebra.SharedCollection.Services[NineCardsServices]]
    implicit val subscriptionServices = mock[algebra.Subscription.Services[NineCardsServices]]
    implicit val userServices = mock[algebra.User.Services[NineCardsServices]]
    implicit val firebaseServices = mock[Firebase.Services[NineCardsServices]]
    implicit val googlePlayServices = mock[GooglePlay.Services[NineCardsServices]]

    implicit val sharedCollectionProcesses = SharedCollectionProcesses.processes[NineCardsServices]

    def mockGetSubscription(res: Option[SharedCollectionSubscription]) = {
      subscriptionServices
        .getByCollectionAndUser(any, any) returns
        Free.pure(res)
    }

    def mockRemoveSubscription(res: Int) = {
      subscriptionServices
        .removeByCollectionAndUser(any, any) returns
        Free.pure(res)
    }

  }

  trait SharedCollectionSuccessfulScope extends BasicScope {

    collectionServices.add(collection = mockEq(sharedCollectionDataServices)) returns
      Free.pure(collection)

    collectionServices.getByPublicId(
      publicId = publicIdentifier
    ) returns Free.pure(Option(collection))

    collectionServices.getLatestByCategory(
      category   = category,
      pageParams = pageParams
    ) returns Free.pure(List(collection))

    collectionServices.getByUser(
      user = publisherId
    ) returns Free.pure(List(collectionWithSubscriptions))

    collectionServices.getTopByCategory(
      category   = category,
      pageParams = pageParams
    ) returns Free.pure(List(collection))

    collectionServices.getByUser(
      user = subscriberId
    ) returns Free.pure(List[SharedCollectionWithAggregatedInfo]())

    collectionServices.update(
      id    = collectionId,
      title = name
    ) returns Free.pure(updatedCollectionsCount)

    collectionServices.updatePackages(
      collection = collectionId,
      packages   = updatePackagesName
    ) returns Free.pure(updatedPackages)

    firebaseServices.sendUpdatedCollectionNotification(any) returns Free.pure(Xor.right(notificationResponse))

    subscriptionServices
      .add(any, any, any) returns
      Free.pure(updatedSubscriptionsCount)

    subscriptionServices
      .getByUser(any) returns
      Free.pure(List(subscription))

    googlePlayServices.resolveManyDetailed(any, any) returns Free.pure(appsInfo)
    googlePlayServices.resolveManyBasic(any, any) returns Free.pure(appsInfoBasic)

    userServices.getSubscribedInstallationByCollection(any) returns
      Free.pure(List(installation))
  }

  trait SharedCollectionUnsuccessfulScope extends BasicScope {

    collectionServices.getByPublicId(publicId = publicIdentifier) returns
      Free.pure(nonExistentSharedCollection)
  }

}

class SharedCollectionProcessesSpec
  extends SharedCollectionProcessesSpecification
  with ScalaCheck {

  "createCollection" should {
    "return a valid response info when the shared collection is created" in
      new SharedCollectionSuccessfulScope {
        val response = sharedCollectionProcesses.createCollection(
          request = createCollectionRequest
        )

        response.foldMap(testInterpreters) must_== createCollectionResponse
      }
  }

  "getCollectionByPublicIdentifier" should {
    "return a valid shared collection info when the shared collection exists" in
      new SharedCollectionSuccessfulScope {
        val collectionInfo = sharedCollectionProcesses.getCollectionByPublicIdentifier(
          userId           = publisherId,
          publicIdentifier = publicIdentifier,
          marketAuth       = marketAuth
        )

        collectionInfo.foldMap(testInterpreters) must beXorRight(getCollectionByPublicIdentifierResponse)
      }

    "return a SharedCollectionNotFoundException when the shared collection doesn't exist" in
      new SharedCollectionUnsuccessfulScope {
        val collectionInfo = sharedCollectionProcesses.getCollectionByPublicIdentifier(
          userId           = publisherId,
          publicIdentifier = publicIdentifier,
          marketAuth       = marketAuth
        )

        collectionInfo.foldMap(testInterpreters) must beXorLeft(sharedCollectionNotFoundException)
      }
  }

  "getLatestCollectionsByCategory" should {

    "return a list of Shared collections of the given category" in new SharedCollectionSuccessfulScope {
      val response = GetCollectionsResponse(List(sharedCollectionWithAppsInfoBasic))
      val collectionsInfo = sharedCollectionProcesses.getLatestCollectionsByCategory(
        userId     = publisherId,
        category   = category,
        marketAuth = marketAuth,
        pageParams = pageParams
      )
      collectionsInfo.foldMap(testInterpreters) mustEqual response
    }

  }

  "getPublishedCollections" should {

    "return a list of Shared collections of the publisher user" in new SharedCollectionSuccessfulScope {
      val response = GetCollectionsResponse(List(sharedCollectionWithAppsInfoAndSubscriptions))
      val collectionsInfo = sharedCollectionProcesses.getPublishedCollections(
        userId     = publisherId,
        marketAuth = marketAuth
      )
      collectionsInfo.foldMap(testInterpreters) mustEqual response
    }

    "return a list of Shared collections of the subscriber user" in new SharedCollectionSuccessfulScope {
      val response = GetCollectionsResponse(List())
      val collectionsInfo = sharedCollectionProcesses.getPublishedCollections(
        userId     = subscriberId,
        marketAuth = marketAuth
      )
      collectionsInfo.foldMap(testInterpreters) mustEqual response
    }

  }

  "getSubscriptionsByUser" should {

    "return a list of public identifiers of collections which the user is subscribed to" in new SharedCollectionSuccessfulScope {
      val response = GetSubscriptionsByUserResponse(List(publicIdentifier))
      val subscriptions = sharedCollectionProcesses.getSubscriptionsByUser(
        user = subscriberId
      )
      subscriptions.foldMap(testInterpreters) mustEqual response
    }

  }

  "getTopCollectionsByCategory" should {

    "return a list of Shared collections of the given category" in new SharedCollectionSuccessfulScope {
      val response = GetCollectionsResponse(List(sharedCollectionWithAppsInfoBasic))
      val collectionsInfo = sharedCollectionProcesses.getTopCollectionsByCategory(
        userId     = publisherId,
        category   = category,
        marketAuth = marketAuth,
        pageParams = pageParams
      )
      collectionsInfo.foldMap(testInterpreters) mustEqual response
    }

  }

  "subscribe" should {

    "return a SharedCollectionNotFoundException when the shared collection does not exist" in new SharedCollectionUnsuccessfulScope {
      val subscriptionInfo = sharedCollectionProcesses.subscribe(publicIdentifier, subscriberId)
      subscriptionInfo.foldMap(testInterpreters) must beXorLeft(sharedCollectionNotFoundException)
    }

    "return a valid response if the subscription already exists  " in new SharedCollectionSuccessfulScope {
      mockGetSubscription(Option(subscription))

      val subscriptionInfo = sharedCollectionProcesses.subscribe(publicIdentifier, subscriberId)
      subscriptionInfo.foldMap(testInterpreters) must beXorRight(SubscribeResponse())
    }

    "return a valid response if it has created a subscription " in new SharedCollectionSuccessfulScope {
      mockGetSubscription(None)

      val subscriptionInfo = sharedCollectionProcesses.subscribe(publicIdentifier, subscriberId)
      subscriptionInfo.foldMap(testInterpreters) must beXorRight(SubscribeResponse())
    }

  }

  "unsubscribe" should {

    "return a SharedCollectionNotFoundException when the shared collection does not exist" in new SharedCollectionUnsuccessfulScope {
      val subscriptionInfo = sharedCollectionProcesses.unsubscribe(publicIdentifier, subscriberId)
      subscriptionInfo.foldMap(testInterpreters) must beXorLeft(sharedCollectionNotFoundException)
    }

    "return a valid response if the subscription existed" in new SharedCollectionSuccessfulScope {
      mockRemoveSubscription(1)

      val subscriptionInfo = sharedCollectionProcesses.unsubscribe(publicIdentifier, subscriberId)
      subscriptionInfo.foldMap(testInterpreters) must beXorRight(UnsubscribeResponse())
    }

    "return a valid response if the subscription did not existed " in new SharedCollectionSuccessfulScope {
      mockRemoveSubscription(0)

      val subscriptionInfo = sharedCollectionProcesses.unsubscribe(publicIdentifier, subscriberId)
      subscriptionInfo.foldMap(testInterpreters) must beXorRight(UnsubscribeResponse())
    }

  }

  "update" should {
    "return the public identifier and the added and removed packages if the shared collection exists" in
      new SharedCollectionSuccessfulScope {
        val collectionInfo = sharedCollectionProcesses.updateCollection(
          publicIdentifier = publicIdentifier,
          collectionInfo   = Option(sharedCollectionUpdateInfo),
          packages         = Option(updatePackagesName)
        )

        collectionInfo.foldMap(testInterpreters) must beXorRight[CreateOrUpdateCollectionResponse].which {
          response ⇒
            response.publicIdentifier must_== publicIdentifier
            response.packagesStats.added must_== addedPackagesCount
            response.packagesStats.removed must beSome(removedPackagesCount)
        }
      }

    "return added and removed packages counts equal to 0 if the package list is not given" in
      new SharedCollectionSuccessfulScope {
        val collectionInfo = sharedCollectionProcesses.updateCollection(
          publicIdentifier = publicIdentifier,
          collectionInfo   = Option(sharedCollectionUpdateInfo),
          packages         = None
        )

        collectionInfo.foldMap(testInterpreters) must beXorRight[CreateOrUpdateCollectionResponse].which {
          response ⇒
            response.publicIdentifier must_== publicIdentifier
            response.packagesStats.added must_== 0
            response.packagesStats.removed must beSome(0)
        }
      }

    "return a SharedCollectionNotFoundException when the shared collection doesn't exist" in
      new SharedCollectionUnsuccessfulScope {
        val collectionInfo = sharedCollectionProcesses.updateCollection(
          publicIdentifier = publicIdentifier,
          collectionInfo   = Option(sharedCollectionUpdateInfo),
          packages         = Option(updatePackagesName)
        )

        collectionInfo.foldMap(testInterpreters) must beXorLeft(sharedCollectionNotFoundException)
      }
  }
}
