package cards.nine.processes

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.processes.NineCardsServices._
import cards.nine.processes.TestData.Messages._
import cards.nine.processes.TestData.Values._
import cards.nine.processes.TestData._
import cards.nine.processes.messages.SharedCollectionMessages._
import cards.nine.services.free.algebra
import cards.nine.services.free.algebra._
import org.specs2.ScalaCheck
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait SharedCollectionProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with TestInterpreters {

  trait BasicScope extends Scope {

    implicit val collectionServices: algebra.SharedCollection.Services[NineCardsServices] =
      mock[algebra.SharedCollection.Services[NineCardsServices]]
    implicit val subscriptionServices: Subscription.Services[NineCardsServices] =
      mock[Subscription.Services[NineCardsServices]]
    implicit val userServices: User.Services[NineCardsServices] =
      mock[User.Services[NineCardsServices]]
    implicit val firebaseServices: Firebase.Services[NineCardsServices] =
      mock[Firebase.Services[NineCardsServices]]
    implicit val googlePlayServices: GooglePlay.Services[NineCardsServices] =
      mock[GooglePlay.Services[NineCardsServices]]

    val sharedCollectionProcesses = SharedCollectionProcesses.processes[NineCardsServices]
  }

}

class SharedCollectionProcessesSpec
  extends SharedCollectionProcessesSpecification
  with ScalaCheck {

  "createCollection" should {
    "return a valid response info when the shared collection is created" in new BasicScope {

      collectionServices.add(collection = sharedCollectionDataServices) returns
        NineCardsService.right(collection)

      sharedCollectionProcesses
        .createCollection(createCollectionRequest)
        .foldMap(testInterpreters) must beRight(createCollectionResponse)
    }
  }

  "getCollectionByPublicIdentifier" should {
    "return a valid shared collection info when the shared collection exists" in new BasicScope {

      collectionServices.getByPublicId(publicIdentifier) returns NineCardsService.right(collection)
      googlePlayServices.resolveManyDetailed(any, any) returns NineCardsService.right(appsInfo)

      sharedCollectionProcesses.getCollectionByPublicIdentifier(
        userId           = publisherId,
        publicIdentifier = publicIdentifier,
        marketAuth       = marketAuth
      ).foldMap(testInterpreters) must beRight(getCollectionByPublicIdentifierResponse)
    }

    "return a SharedCollectionNotFoundException when the shared collection doesn't exist" in
      new BasicScope {

        collectionServices.getByPublicId(publicId = publicIdentifier) returns
          NineCardsService.left(sharedCollectionNotFoundError)

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

      collectionServices.getLatestByCategory(category, pageParams) returns NineCardsService.right(List(collection))
      googlePlayServices.resolveManyBasic(any, any) returns NineCardsService.right(appsInfoBasic)

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

      collectionServices.getByUser(publisherId) returns NineCardsService.right(List(collectionWithSubscriptions))
      googlePlayServices.resolveManyBasic(any, any) returns NineCardsService.right(appsInfoBasic)

      sharedCollectionProcesses
        .getPublishedCollections(publisherId, marketAuth)
        .foldMap(testInterpreters) must beRight(GetCollectionsResponse(List(sharedCollectionWithAppsInfoAndSubscriptions)))
    }
  }

  "getSubscriptionsByUser" should {

    "return a list of public identifiers of collections which the user is subscribed to" in new BasicScope {

      subscriptionServices.getByUser(subscriberId) returns NineCardsService.right(List(subscription))

      sharedCollectionProcesses
        .getSubscriptionsByUser(subscriberId)
        .foldMap(testInterpreters) must beRight(GetSubscriptionsByUserResponse(List(publicIdentifier)))
    }

  }

  "getTopCollectionsByCategory" should {

    "return a list of Shared collections of the given category" in new BasicScope {
      collectionServices.getTopByCategory(category, pageParams) returns NineCardsService.right(List(collection))
      googlePlayServices.resolveManyBasic(any, any) returns NineCardsService.right(appsInfoBasic)

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

      collectionServices.getByPublicId(publicId = publicIdentifier) returns
        NineCardsService.left(sharedCollectionNotFoundError)

      sharedCollectionProcesses
        .subscribe(publicIdentifier, subscriberId)
        .foldMap(testInterpreters) must beLeft(sharedCollectionNotFoundError)
    }

    "return a valid response if the subscription already exists  " in new BasicScope {

      collectionServices.getByPublicId(publicIdentifier) returns NineCardsService.right(collection)
      subscriptionServices.getByCollectionAndUser(any, any) returns NineCardsService.right(Option(subscription))

      sharedCollectionProcesses
        .subscribe(publicIdentifier, subscriberId)
        .foldMap(testInterpreters) must beRight(SubscribeResponse())
    }

    "return a valid response if it has created a subscription " in new BasicScope {

      collectionServices.getByPublicId(publicIdentifier) returns NineCardsService.right(collection)
      subscriptionServices.add(any, any, any) returns NineCardsService.right(updatedSubscriptionsCount)
      subscriptionServices.getByCollectionAndUser(any, any) returns NineCardsService.right(None)

      sharedCollectionProcesses
        .subscribe(publicIdentifier, subscriberId)
        .foldMap(testInterpreters) must beRight(SubscribeResponse())
    }

  }

  "unsubscribe" should {

    "return a SharedCollectionNotFound error when the shared collection does not exist" in new BasicScope {
      collectionServices.getByPublicId(publicId = publicIdentifier) returns
        NineCardsService.left(sharedCollectionNotFoundError)

      val subscriptionInfo = sharedCollectionProcesses.unsubscribe(publicIdentifier, subscriberId)
      subscriptionInfo.foldMap(testInterpreters) must beLeft(sharedCollectionNotFoundError)
    }

    "return a valid response if the subscription existed" in new BasicScope {

      collectionServices.getByPublicId(publicIdentifier) returns NineCardsService.right(collection)
      subscriptionServices.removeByCollectionAndUser(any, any) returns NineCardsService.right(1)

      val subscriptionInfo = sharedCollectionProcesses.unsubscribe(publicIdentifier, subscriberId)
      subscriptionInfo.foldMap(testInterpreters) must beRight(UnsubscribeResponse())
    }

    "return a valid response if the subscription did not existed " in new BasicScope {

      collectionServices.getByPublicId(publicIdentifier) returns NineCardsService.right(collection)
      subscriptionServices.removeByCollectionAndUser(any, any) returns NineCardsService.right(0)

      val subscriptionInfo = sharedCollectionProcesses.unsubscribe(publicIdentifier, subscriberId)
      subscriptionInfo.foldMap(testInterpreters) must beRight(UnsubscribeResponse())
    }

  }

  "increaseViewsCountByOne" should {
    "increase the number of views by 1 if the shared collection exists" in
      new BasicScope {

        collectionServices.getByPublicId(publicIdentifier) returns NineCardsService.right(collection)
        collectionServices.increaseViewsByOne(id = collectionId) returns NineCardsService.right(updatedCollectionsCount)

        val collectionInfo = sharedCollectionProcesses.increaseViewsCountByOne(publicIdentifier)

        collectionInfo.foldMap(testInterpreters) must beRight[IncreaseViewsCountByOneResponse].which {
          response ⇒ response.publicIdentifier must_== publicIdentifier
        }
      }

    "return a SharedCollectionNotFound error when the shared collection doesn't exist" in
      new BasicScope {

        collectionServices.getByPublicId(publicId = publicIdentifier) returns
          NineCardsService.left(sharedCollectionNotFoundError)

        val collectionInfo = sharedCollectionProcesses.increaseViewsCountByOne(publicIdentifier)

        collectionInfo.foldMap(testInterpreters) must beLeft(sharedCollectionNotFoundError)
      }
  }

  "update" should {
    "return the public identifier and the added and removed packages if the shared collection exists" in
      new BasicScope {

        collectionServices.getByPublicId(publicIdentifier) returns NineCardsService.right(collection)
        collectionServices.update(collectionId, name) returns NineCardsService.right(updatedCollectionsCount)
        collectionServices.updatePackages(collectionId, updatePackagesName) returns NineCardsService.right(updatedPackages)
        firebaseServices.sendUpdatedCollectionNotification(any) returns NineCardsService.right(sendNotificationResponse)
        userServices.getSubscribedInstallationByCollection(any) returns NineCardsService.right(List(installation))

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

        collectionServices.getByPublicId(publicIdentifier) returns NineCardsService.right(collection)
        collectionServices.update(collectionId, name) returns NineCardsService.right(updatedCollectionsCount)

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

        collectionServices.getByPublicId(publicId = publicIdentifier) returns
          NineCardsService.left(sharedCollectionNotFoundError)

        sharedCollectionProcesses.updateCollection(
          publicIdentifier = publicIdentifier,
          collectionInfo   = Option(sharedCollectionUpdateInfo),
          packages         = Option(updatePackagesName)
        ).foldMap(testInterpreters) must beLeft(sharedCollectionNotFoundError)
      }
  }
}
