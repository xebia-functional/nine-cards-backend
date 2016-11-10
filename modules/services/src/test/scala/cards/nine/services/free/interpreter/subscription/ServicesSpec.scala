package cards.nine.services.free.interpreter.subscription

import cards.nine.services.free.domain.{ SharedCollection, SharedCollectionSubscription, User }
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import cards.nine.services.free.interpreter.user.Services.UserData
import cards.nine.services.persistence.{ DomainDatabaseContext, NineCardsScalacheckGen }
import doobie.contrib.postgresql.pgtypes._
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import shapeless.syntax.std.product._

class ServicesSpec
  extends Specification
  with ScalaCheck
  with DomainDatabaseContext
  with DisjunctionMatchers
  with NineCardsScalacheckGen {

  sequential

  def generateSubscription(userData: UserData, collectionData: SharedCollectionData) = {
    for {
      _ ← deleteAllRows
      u ← insertItem(User.Queries.insert, userData.toTuple)
      c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
      _ ← insertItemWithoutGeneratedKeys(
        sql    = SharedCollectionSubscription.Queries.insert,
        values = (c, u, collectionData.publicIdentifier)
      )
    } yield (u, c)
  }.transactAndRun

  "addSubscription" should {
    "create a new subscriptions when an existing user and shared collection is given" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (userId: Long, collectionId: Long) = (for {
          _ ← deleteAllRows
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield (u, c)).transactAndRun

        subscriptionPersistenceServices.add(
          collectionId       = collectionId,
          userId             = userId,
          collectionPublicId = collectionData.publicIdentifier
        ).transactAndRun

        val storedSubscription =
          subscriptionPersistenceServices.getByCollectionAndUser(
            collectionId = collectionId,
            userId       = userId
          ).transactAndRun

        storedSubscription must beSome[SharedCollectionSubscription].which { subscription ⇒
          subscription.sharedCollectionId must_== collectionId
          subscription.userId must_== userId
        }
      }
    }
  }

  "getSubscriptionByCollection" should {
    "return an empty list if the table is empty" in {
      prop { (collectionId: Long) ⇒
        WithEmptyDatabase {
          val subscriptions =
            subscriptionPersistenceServices.getByCollection(
              collectionId = collectionId
            ).transactAndRun

          subscriptions must beEmpty
        }
      }
    }
    "return a list of subscriptions associated with the given collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (_, collectionId) = generateSubscription(userData, collectionData)

        val storedSubscriptions =
          subscriptionPersistenceServices.getByCollection(
            collectionId = collectionId
          ).transactAndRun

        storedSubscriptions must contain { subscription: SharedCollectionSubscription ⇒
          subscription.sharedCollectionId must_== collectionId
        }.forall
      }
    }
    "return an empty list if there isn't any subscription associated with the given collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (_, collectionId) = generateSubscription(userData, collectionData)

        val subscriptions = subscriptionPersistenceServices.getByCollection(
          collectionId = collectionId + 1000000
        ).transactAndRun

        subscriptions must beEmpty
      }
    }
  }

  "getSubscriptionByCollectionAndUser" should {
    "return None if the table is empty" in {
      prop { (userId: Long, collectionId: Long) ⇒
        WithEmptyDatabase {
          val subscription = subscriptionPersistenceServices.getByCollectionAndUser(
            collectionId = collectionId,
            userId       = userId
          ).transactAndRun

          subscription must beNone
        }
      }
    }
    "return a subscription if there is a record for the given user and collection in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (userId, collectionId) = generateSubscription(userData, collectionData)

        val subscription = subscriptionPersistenceServices.getByCollectionAndUser(
          collectionId = collectionId,
          userId       = userId
        ).transactAndRun

        subscription must beSome[SharedCollectionSubscription].which { s ⇒
          s.sharedCollectionId must_== collectionId
          s.userId must_== userId
        }
      }
    }
    "return None if there isn't any subscription for the given user and collection in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (userId, collectionId) = generateSubscription(userData, collectionData)

        val subscription = subscriptionPersistenceServices.getByCollectionAndUser(
          collectionId = collectionId + 1000000,
          userId       = userId + 1000000
        ).transactAndRun

        subscription must beNone
      }
    }
  }

  "getSubscriptionByUser" should {
    "return an empty list if the table is empty" in {
      prop { (userId: Long) ⇒
        WithEmptyDatabase {
          val subscriptions = subscriptionPersistenceServices.getByUser(
            userId = userId
          ).transactAndRun

          subscriptions must beEmpty
        }
      }
    }
    "return a list of subscriptions associated for the given user" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (userId, _) = generateSubscription(userData, collectionData)

        val storedSubscriptions = subscriptionPersistenceServices.getByUser(
          userId = userId
        ).transactAndRun

        storedSubscriptions must contain { subscription: SharedCollectionSubscription ⇒
          subscription.userId must_== userId
        }.forall
      }
    }
    "return an empty list if there isn't any subscription associated for the given user" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (userId, _) = generateSubscription(userData, collectionData)

        val subscriptions = subscriptionPersistenceServices.getByUser(
          userId = userId + 1000000
        ).transactAndRun

        subscriptions must beEmpty
      }
    }
  }

  "removeSubscriptionByCollectionAndUser" should {
    "return 0 there isn't any subscription for the given user and collection in the database" in {
      prop { (userId: Long, collectionId: Long) ⇒
        WithEmptyDatabase {
          val deleted = subscriptionPersistenceServices.removeByCollectionAndUser(
            collectionId = collectionId,
            userId       = userId
          ).transactAndRun

          deleted must_== 0
        }
      }
    }
    "return 1 if there is a subscription for the given user and collection in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (userId, collectionId) = generateSubscription(userData, collectionData)

        val deleted = subscriptionPersistenceServices.removeByCollectionAndUser(
          collectionId = collectionId,
          userId       = userId
        ).transactAndRun

        deleted must_== 1
      }
    }
  }

}
