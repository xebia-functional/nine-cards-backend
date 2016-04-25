package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain._
import com.fortysevendeg.ninecards.services.persistence.SharedCollectionPersistenceServices.SharedCollectionData
import com.fortysevendeg.ninecards.services.persistence.UserPersistenceServices.UserData
import doobie.imports._
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import shapeless.syntax.std.product._

class SharedCollectionSubscriptionPersistenceServicesSpec
  extends Specification
  with BeforeEach
  with ScalaCheck
  with DomainDatabaseContext
  with DisjunctionMatchers
  with NineCardsScalacheckGen {

  sequential

  def before = {
    flywaydb.clean()
    flywaydb.migrate()
  }

  "addSubscription" should {
    "create a new subscriptions when an existing user and shared collection is given" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (userId: Long, collectionId: Long) = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield (u, c)).transact(transactor).run

        val id: Long = scSubscriptionPersistenceServices.addSubscription[Long](
          collectionId = collectionId,
          userId       = userId
        ).transact(transactor).run

        val storedSubscription =
          scSubscriptionPersistenceServices.getSubscriptionById(id).transact(transactor).run

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
        val subscriptions =
          scSubscriptionPersistenceServices.getSubscriptionsByCollection(
            collectionId = collectionId
          ).transact(transactor).run

        subscriptions must beEmpty
      }
    }
    "return a list of subscriptions associated with the given collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val collectionId = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          s ← insertItem(SharedCollectionSubscription.Queries.insert, (c, u))
        } yield c).transact(transactor).run

        val storedSubscriptions =
          scSubscriptionPersistenceServices.getSubscriptionsByCollection(
            collectionId = collectionId
          ).transact(transactor).run

        storedSubscriptions must contain { subscription: SharedCollectionSubscription ⇒
          subscription.sharedCollectionId must_== collectionId
        }.forall
      }
    }
    "return an empty list if there isn't any subscription associated with the given collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val collectionId = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          _ ← insertItem(SharedCollectionSubscription.Queries.insert, (c, u))
        } yield c).transact(transactor).run

        val subscriptions = scSubscriptionPersistenceServices.getSubscriptionsByCollection(
          collectionId = collectionId + 1000000
        ).transact(transactor).run

        subscriptions must beEmpty
      }
    }
  }

  "getSubscriptionByCollectionAndUser" should {
    "return None if the table is empty" in {
      prop { (userId: Long, collectionId: Long) ⇒
        val subscription = scSubscriptionPersistenceServices.getSubscriptionByCollectionAndUser(
          collectionId = collectionId,
          userId       = userId
        ).transact(transactor).run

        subscription must beNone
      }
    }
    "return a subscription if there is a record for the given user and collection in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (userId: Long, collectionId: Long) = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          _ ← insertItem(SharedCollectionSubscription.Queries.insert, (c, u))
        } yield (u, c)).transact(transactor).run

        val subscription = scSubscriptionPersistenceServices.getSubscriptionByCollectionAndUser(
          collectionId = collectionId,
          userId       = userId
        ).transact(transactor).run

        subscription must beSome[SharedCollectionSubscription].which { s ⇒
          s.sharedCollectionId must_== collectionId
          s.userId must_== userId
        }
      }
    }
    "return None if there isn't any subscription for the given user and collection in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (userId: Long, collectionId: Long) = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          _ ← insertItem(SharedCollectionSubscription.Queries.insert, (c, u))
        } yield (u, c)).transact(transactor).run

        val subscription = scSubscriptionPersistenceServices.getSubscriptionByCollectionAndUser(
          collectionId = collectionId + 1000000,
          userId       = userId + 1000000
        ).transact(transactor).run

        subscription must beNone
      }
    }
  }

  "getSubscriptionById" should {
    "return None if the table is empty" in {
      prop { (id: Long) ⇒
        val subscription = scSubscriptionPersistenceServices.getSubscriptionById(
          subscriptionId = id
        ).transact(transactor).run

        subscription must beNone
      }
    }
    "return a subscription if there is a record for the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (userId: Long, collectionId: Long, id: Long) = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          s ← insertItem(SharedCollectionSubscription.Queries.insert, (c, u))
        } yield (u, c, s)).transact(transactor).run

        val storedSubscription = scSubscriptionPersistenceServices.getSubscriptionById(
          subscriptionId = id
        ).transact(transactor).run

        storedSubscription must beSome[SharedCollectionSubscription].which { subscription ⇒
          subscription.sharedCollectionId must_== collectionId
          subscription.userId must_== userId
        }
      }
    }
    "return None if there isn't any subscription for the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val id = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          s ← insertItem(SharedCollectionSubscription.Queries.insert, (c, u))
        } yield s).transact(transactor).run

        val storedSubscription = scSubscriptionPersistenceServices.getSubscriptionById(
          subscriptionId = id + 1000000
        ).transact(transactor).run

        storedSubscription must beNone
      }
    }
  }

  "getSubscriptionByUser" should {
    "return an empty list if the table is empty" in {
      prop { (userId: Long) ⇒
        val subscriptions = scSubscriptionPersistenceServices.getSubscriptionsByUser(
          userId = userId
        ).transact(transactor).run

        subscriptions must beEmpty
      }
    }
    "return a list of subscriptions associated for the given user" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val userId = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          _ ← insertItem(SharedCollectionSubscription.Queries.insert, (c, u))
        } yield u).transact(transactor).run

        val storedSubscriptions = scSubscriptionPersistenceServices.getSubscriptionsByUser(
          userId = userId
        ).transact(transactor).run

        storedSubscriptions must contain { subscription: SharedCollectionSubscription ⇒
          subscription.userId must_== userId
        }.forall
      }
    }
    "return an empty list if there isn't any subscription associated for the given user" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val userId = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          _ ← insertItem(SharedCollectionSubscription.Queries.insert, (c, u))
        } yield u).transact(transactor).run

        val subscriptions = scSubscriptionPersistenceServices.getSubscriptionsByUser(
          userId = userId + 1000000
        ).transact(transactor).run

        subscriptions must beEmpty
      }
    }
  }

  "removeSubscription" should {
    "return 0 there isn't any subscription for the given id in the database" in {
      prop { (id: Long) ⇒
        val count = scSubscriptionPersistenceServices.removeSubscription(
          subscriptionId = id
        ).transact(transactor).run

        count must_== 0
      }
    }
    "return 1 if there is a subscription for the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val id = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          s ← insertItem(SharedCollectionSubscription.Queries.insert, (c, u))
        } yield s).transact(transactor).run

        val deleted = scSubscriptionPersistenceServices.removeSubscription(
          subscriptionId = id
        ).transact(transactor).run

        deleted must_== 1
      }
    }
  }

  "removeSubscriptionByCollectionAndUser" should {
    "return 0 there isn't any subscription for the given user and collection in the database" in {
      prop { (userId: Long, collectionId: Long) ⇒
        val deleted = scSubscriptionPersistenceServices.removeSubscriptionByCollectionAndUser(
          collectionId = collectionId,
          userId       = userId
        ).transact(transactor).run

        deleted must_== 0
      }
    }
    "return 1 if there is a subscription for the given user and collection in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val (userId: Long, collectionId: Long) = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          s ← insertItem(SharedCollectionSubscription.Queries.insert, (c, u))
        } yield (u, c)).transact(transactor).run

        val deleted = scSubscriptionPersistenceServices.removeSubscriptionByCollectionAndUser(
          collectionId = collectionId,
          userId       = userId
        ).transact(transactor).run

        deleted must_== 1
      }
    }
  }
}