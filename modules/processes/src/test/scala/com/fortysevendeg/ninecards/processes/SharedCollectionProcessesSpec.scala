package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import com.fortysevendeg.ninecards.processes.TestData.Exceptions._
import com.fortysevendeg.ninecards.processes.TestData.Messages._
import com.fortysevendeg.ninecards.processes.TestData.Values._
import com.fortysevendeg.ninecards.processes.TestData._
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.utils.DummyNineCardsConfig
import com.fortysevendeg.ninecards.services.free.algebra.GooglePlay
import com.fortysevendeg.ninecards.services.free.domain.{
  SharedCollectionSubscription,
  SharedCollection ⇒ SharedCollectionServices
}
import com.fortysevendeg.ninecards.services.persistence._
import doobie.imports._
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.ScalaCheck
import org.specs2.matcher.{ Matchers, XorMatchers }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.Scalaz._

trait SharedCollectionProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with DummyNineCardsConfig
  with XorMatchers
  with TestInterpreters {

  trait BasicScope extends Scope {

    implicit val sharedCollectionPersistenceServices = mock[SharedCollectionPersistenceServices]
    implicit val sharedCollectionSubscriptionPersistenceServices = mock[SharedCollectionSubscriptionPersistenceServices]
    implicit val googlePlayServices = mock[GooglePlay.Services[NineCardsServices]]
    implicit val sharedCollectionProcesses = new SharedCollectionProcesses[NineCardsServices]

    def mockGetSubscription(res: Option[SharedCollectionSubscription]) = {
      sharedCollectionSubscriptionPersistenceServices
        .getSubscriptionByCollectionAndUser(any, any) returns
        res.point[ConnectionIO]
    }

    def mockRemoveSubscription(res: Int) = {
      sharedCollectionSubscriptionPersistenceServices
        .removeSubscriptionByCollectionAndUser(any, any) returns
        res.point[ConnectionIO]
    }

  }

  trait SharedCollectionSuccessfulScope extends BasicScope {

    sharedCollectionPersistenceServices.addCollection[SharedCollectionServices](
      data = mockEq(sharedCollectionDataServices)
    )(any) returns collection.point[ConnectionIO]

    sharedCollectionPersistenceServices.addPackages(
      collectionId = collectionId,
      packagesName = packagesName
    ) returns addedPackages.point[ConnectionIO]

    sharedCollectionPersistenceServices.getCollectionByPublicIdentifier(
      publicIdentifier = publicIdentifier
    ) returns Option(collection).point[ConnectionIO]

    sharedCollectionPersistenceServices.getPackagesByCollection(
      collectionId = collectionId
    ) returns packages.point[ConnectionIO]

    sharedCollectionPersistenceServices.getCollectionsByUserId(
      userId = publisherId
    ) returns List(collection).point[ConnectionIO]

    sharedCollectionPersistenceServices.getCollectionsByUserId(
      userId = subscriberId
    ) returns List[SharedCollectionServices]().point[ConnectionIO]

    sharedCollectionPersistenceServices.updateCollectionInfo(
      id          = collectionId,
      title       = name,
      description = description
    ) returns updatedCollectionsCount.point[ConnectionIO]

    sharedCollectionPersistenceServices.updatePackages(
      collectionId = collectionId,
      packages     = updatePackagesName
    ) returns updatedPackagesCount.point[ConnectionIO]

    sharedCollectionSubscriptionPersistenceServices
      .addSubscription[SharedCollectionSubscription](any, any)(any) returns
      subscription.point[ConnectionIO]

    googlePlayServices.resolveMany(any, any) returns Free.pure(appsInfo)
  }

  trait SharedCollectionUnsuccessfulScope extends BasicScope {

    sharedCollectionPersistenceServices.getCollectionByPublicIdentifier(
      publicIdentifier = publicIdentifier
    ) returns nonExistentSharedCollection.point[ConnectionIO]
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
          publicIdentifier = publicIdentifier,
          authParams       = authParams
        )

        collectionInfo.foldMap(testInterpreters) must beXorRight(getCollectionByPublicIdentifierResponse)
      }

    "return a SharedCollectionNotFoundException when the shared collection doesn't exist" in
      new SharedCollectionUnsuccessfulScope {
        val collectionInfo = sharedCollectionProcesses.getCollectionByPublicIdentifier(
          publicIdentifier = publicIdentifier,
          authParams       = authParams
        )

        collectionInfo.foldMap(testInterpreters) must beXorLeft(sharedCollectionNotFoundException)
      }
  }

  "getPublishedCollections" should {

    "return a list of Shared collections of the publisher user" in new SharedCollectionSuccessfulScope {
      val response = GetPublishedCollectionsResponse(List(sharedCollectionWithAppsInfo))
      val collectionsInfo = sharedCollectionProcesses.getPublishedCollections(
        userId     = publisherId,
        authParams = authParams
      )
      collectionsInfo.foldMap(testInterpreters) mustEqual response
    }

    "return a list of Shared collections of the subscriber user" in new SharedCollectionSuccessfulScope {
      val response = GetPublishedCollectionsResponse(List())
      val collectionsInfo = sharedCollectionProcesses.getPublishedCollections(
        userId     = subscriberId,
        authParams = authParams
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
            response.packagesInfo.added must_== addedPackages
            response.packagesInfo.removed must beSome(removedPackages)
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
            response.packagesInfo.added must_== 0
            response.packagesInfo.removed must beSome(0)
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
