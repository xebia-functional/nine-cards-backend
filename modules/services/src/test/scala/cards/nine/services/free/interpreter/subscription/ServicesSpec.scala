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
package cards.nine.services.free.interpreter.subscription

import cards.nine.commons.catscalaz.ScalazInstances
import cards.nine.services.free.domain.SharedCollectionSubscription.Queries.{ insert ⇒ subscriptionInsert }
import cards.nine.services.free.domain.{ SharedCollection, SharedCollectionSubscription, User }
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import cards.nine.services.free.interpreter.user.Services.UserData
import cards.nine.services.persistence.{ DomainDatabaseContext, NineCardsScalacheckGen }
import cats.Applicative
import doobie.contrib.postgresql.pgtypes._
import cats.instances.list._
import cats.syntax.traverse._
import doobie.imports.ConnectionIO
import org.specs2.ScalaCheck
import org.specs2.matcher.{ DisjunctionMatchers, MatchResult }
import org.specs2.mutable.Specification
import shapeless.syntax.std.product._

class ServicesSpec
  extends Specification
  with ScalaCheck
  with DomainDatabaseContext
  with DisjunctionMatchers
  with NineCardsScalacheckGen {

  implicit val connectionIOApplicative: Applicative[ConnectionIO] =
    ScalazInstances[ConnectionIO].applicativeInstance

  object WithData {

    private[this] def generateSubscription(
      subscriberData: UserData,
      collection: Long,
      collectionPublicId: String
    ) =
      for {
        s ← insertItem(User.Queries.insert, subscriberData.toTuple)
        _ ← insertItemWithoutGeneratedKeys(subscriptionInsert, (collection, s, collectionPublicId))
      } yield s

    private[this] def generateSubscription(subscriber: Long, collection: SharedCollectionData) =
      for {
        c ← insertItem(SharedCollection.Queries.insert, collection.toTuple)
        _ ← insertItemWithoutGeneratedKeys(subscriptionInsert, (c, subscriber, collection.publicIdentifier))
      } yield c

    def apply[A](
      publisherData: UserData,
      collectionData: SharedCollectionData,
      subscriberData: UserData,
      generateSubscription: Boolean = true
    )(check: (Long, Long) ⇒ MatchResult[A]) = {

      val (collection, subscriber) = (for {
        _ ← deleteAllRows
        publisher ← insertItem(User.Queries.insert, publisherData.toTuple)
        c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(publisher)).toTuple)
        s ← insertItem(User.Queries.insert, subscriberData.toTuple)
        _ ← if (generateSubscription)
          insertItemWithoutGeneratedKeys(subscriptionInsert, (c, s, collectionData.publicIdentifier))
        else
          connectionIOApplicative.pure(0)
      } yield (c, s)).transactAndRun

      check(collection, subscriber)
    }

    def apply[A](publisherData: UserData, collectionData: SharedCollectionData, userData: List[UserData])(check: (Long, Int) ⇒ MatchResult[A]) = {

      val (collection, subscriptionCount) = (for {
        publisher ← insertItem(User.Queries.insert, publisherData.toTuple)
        collection ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(publisher)).toTuple)
        subscriptions ← userData.traverse[ConnectionIO, Long](user ⇒ generateSubscription(user, collection, collectionData.publicIdentifier))
      } yield (collection, subscriptions.size)).transactAndRun

      check(collection, subscriptionCount)
    }

    def apply[A](publisherData: UserData, collectionsData: List[SharedCollectionData], userData: UserData)(check: (Long, Int) ⇒ MatchResult[A]) = {

      val (user, subscriptionCount) = (for {
        publisher ← insertItem(User.Queries.insert, publisherData.toTuple)
        user ← insertItem(User.Queries.insert, userData.toTuple)
        subscriptions ← collectionsData.traverse[ConnectionIO, Long](collection ⇒ generateSubscription(user, collection.copy(userId = Option(publisher))))
      } yield (user, subscriptions.size)).transactAndRun

      check(user, subscriptionCount)
    }
  }

  sequential

  "addSubscription" should {
    "create a new subscriptions when an existing user and shared collection is given" in {
      prop { (publisherData: UserData, collectionData: SharedCollectionData, subscriberData: UserData) ⇒

        WithData(publisherData, collectionData, subscriberData, false) { (collection, subscriber) ⇒

          subscriptionPersistenceServices.add(
            collectionId       = collection,
            userId             = subscriber,
            collectionPublicId = collectionData.publicIdentifier
          ).transactAndRun

          val subscription = getOptionalItem[(Long, Long), SharedCollectionSubscription](
            sql    = SharedCollectionSubscription.Queries.getByCollectionAndUser,
            values = (collection, subscriber)
          ).transactAndRun

          subscription must beSome[SharedCollectionSubscription].which { subscription ⇒
            subscription.sharedCollectionId must_== collection
            subscription.userId must_== subscriber
          }
        }
      }
    }
  }

  "getSubscriptionsByCollection" should {
    "return an empty list if the table is empty" in {
      prop { (collection: Long) ⇒
        WithEmptyDatabase {
          val subscriptions =
            subscriptionPersistenceServices.getByCollection(
              collectionId = collection
            ).transactAndRun

          subscriptions must beRight[List[SharedCollectionSubscription]](Nil)
        }
      }
    }
    "return a list of subscriptions associated with the given collection" in {
      prop { (publisher: UserData, collectionData: SharedCollectionData, users: List[UserData]) ⇒

        WithData(publisher, collectionData, users) { (collection, subscriptionsCount) ⇒

          val subscriptions =
            subscriptionPersistenceServices.getByCollection(
              collectionId = collection
            ).transactAndRun

          subscriptions must beRight[List[SharedCollectionSubscription]].which { list ⇒
            list must haveSize(subscriptionsCount)
            list must contain { subscription: SharedCollectionSubscription ⇒
              subscription.sharedCollectionId must_== collection
            }.forall
          }
        }
      }
    }
    "return an empty list if there isn't any subscription associated with the given collection" in {
      prop { (publisher: UserData, collectionData: SharedCollectionData, users: List[UserData]) ⇒

        WithData(publisher, collectionData, users) { (collection, _) ⇒

          val subscriptions = subscriptionPersistenceServices.getByCollection(
            collectionId = collection * -1
          ).transactAndRun

          subscriptions must beRight[List[SharedCollectionSubscription]](Nil)
        }
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

          subscription must beRight[Option[SharedCollectionSubscription]](None)
        }
      }
    }
    "return a subscription if there is a record for the given user and collection in the database" in {
      prop { (publisherData: UserData, collectionData: SharedCollectionData, subscriberData: UserData) ⇒

        WithData(publisherData, collectionData, subscriberData) { (collection, subscriber) ⇒

          val subscription = subscriptionPersistenceServices.getByCollectionAndUser(
            collectionId = collection,
            userId       = subscriber
          ).transactAndRun

          subscription must beRight[Option[SharedCollectionSubscription]].which {
            maybeSubscription ⇒
              maybeSubscription must beSome[SharedCollectionSubscription].which { s ⇒
                s.sharedCollectionId must_== collection
                s.userId must_== subscriber
              }
          }
        }
      }
    }
    "return None if there isn't any subscription for the given user and collection in the database" in {
      prop { (publisherData: UserData, collectionData: SharedCollectionData, subscriberData: UserData) ⇒

        WithData(publisherData, collectionData, subscriberData) { (collection, subscriber) ⇒

          val subscription = subscriptionPersistenceServices.getByCollectionAndUser(
            collectionId = collection,
            userId       = subscriber * -1
          ).transactAndRun

          subscription must beRight[Option[SharedCollectionSubscription]](None)
        }
      }
    }
  }

  "getSubscriptionsByUser" should {
    "return an empty list if the table is empty" in {
      prop { (userId: Long) ⇒
        WithEmptyDatabase {
          val subscriptions = subscriptionPersistenceServices.getByUser(
            userId = userId
          ).transactAndRun

          subscriptions must beRight[List[SharedCollectionSubscription]](Nil)
        }
      }
    }
    "return a list of subscriptions associated for the given user" in {
      prop { (publisherData: UserData, collectionsData: List[SharedCollectionData], subscriberData: UserData) ⇒

        WithData(publisherData, collectionsData, subscriberData) { (user, subscriptionCount) ⇒

          val subscriptions = subscriptionPersistenceServices.getByUser(
            userId = user
          ).transactAndRun

          subscriptions must beRight[List[SharedCollectionSubscription]].which {
            list ⇒
              list must haveSize(subscriptionCount)
              list must contain { subscription: SharedCollectionSubscription ⇒
                subscription.userId must_== user
              }.forall
          }
        }
      }
    }
    "return an empty list if there isn't any subscription associated for the given user" in {
      prop { (publisherData: UserData, collectionsData: List[SharedCollectionData], subscriberData: UserData) ⇒

        WithData(publisherData, collectionsData, subscriberData) { (user, subscriptionCount) ⇒

          val subscriptions = subscriptionPersistenceServices.getByUser(
            userId = user * -1
          ).transactAndRun

          subscriptions must beRight[List[SharedCollectionSubscription]](Nil)
        }
      }
    }
  }

  "removeSubscriptionByCollectionAndUser" should {
    "return 0 there isn't any subscription for the given user and collection in the database" in {
      prop { (publisherData: UserData, collectionData: SharedCollectionData, subscriberData: UserData) ⇒

        WithData(publisherData, collectionData, subscriberData) { (collection, subscriber) ⇒

          val deleted = subscriptionPersistenceServices.removeByCollectionAndUser(
            collectionId = collection,
            userId       = subscriber * -1
          ).transactAndRun

          deleted must beRight[Int](0)
        }
      }
    }
    "return 1 if there is a subscription for the given user and collection in the database" in {
      prop { (publisherData: UserData, collectionData: SharedCollectionData, subscriberData: UserData) ⇒

        WithData(publisherData, collectionData, subscriberData) { (collection, subscriber) ⇒

          val deleted = subscriptionPersistenceServices.removeByCollectionAndUser(
            collectionId = collection,
            userId       = subscriber
          ).transactAndRun

          deleted must beRight[Int](1)
        }
      }
    }
  }

}
