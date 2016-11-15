package cards.nine.services.free.interpreter.collection

import cards.nine.domain.pagination.Page
import cards.nine.services.free.domain.{ SharedCollection, SharedCollectionWithAggregatedInfo, User }
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import cards.nine.services.free.interpreter.user.Services.UserData
import cards.nine.services.persistence.{ DomainDatabaseContext, NineCardsScalacheckGen }
import doobie.contrib.postgresql.pgtypes._
import doobie.imports._
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import shapeless.syntax.std.product._

import scala.annotation.tailrec
import scala.collection.immutable.List
import scalaz.syntax.traverse.ToTraverseOps
import scalaz.std.list._

trait SharedCollectionPersistenceServicesContext extends DomainDatabaseContext {

  val communicationCategory = "COMMUNICATION"

  val pageNumber = 0

  val pageSize = 25

  val pageParams = Page(pageNumber, pageSize)

  val socialCategory = "SOCIAL"

  val deleteSharedCollectionsQuery = "DELETE FROM sharedcollections"

  val deleteUsersQuery = "DELETE FROM users"

  def createCollectionWithUser(
    collectionData: SharedCollectionData,
    userId: Option[Long]
  ): ConnectionIO[Long] =
    insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = userId))

  def createCollectionsWithCategoryAndUser(
    collectionsData: List[SharedCollectionData],
    category: String,
    userId: Option[Long]
  ): ConnectionIO[Int] =
    insertItems(
      sql    = SharedCollection.Queries.insert,
      values = collectionsData.map(_.copy(category = category, userId = userId))
    )

  def createUser(userData: UserData): ConnectionIO[Long] =
    insertItem(User.Queries.insert, userData.toTuple)

  def deleteSharedCollections: ConnectionIO[Int] = deleteItems(deleteSharedCollectionsQuery)

  def deleteUsers: ConnectionIO[Int] = deleteItems(deleteUsersQuery)

  def divideList[A](n: Int, list: List[A]): List[List[A]] = {
    def merge(heads: List[A], tails: List[List[A]]): List[List[A]] = (heads, tails) match {
      case (Nil, Nil) ⇒ Nil
      case (h :: hs, Nil) ⇒ throw new Exception("This should not happen")
      case (Nil, t :: ts) ⇒ tails
      case (h :: hs, t :: ts) ⇒ (h :: t) :: merge(hs, ts)
    }

    @tailrec
    def divideAux(xs: List[A], results: List[List[A]]): List[List[A]] =
      if (xs.isEmpty)
        results map (_.reverse)
      else {
        val (pre, post) = xs.splitAt(n)
        divideAux(post, merge(pre, results))
      }

    divideAux(list, List.fill(n)(Nil))
  }

}
class ServicesSpec
  extends Specification
  with ScalaCheck
  with DisjunctionMatchers
  with NineCardsScalacheckGen
  with SharedCollectionPersistenceServicesContext {

  sequential

  "addCollection" should {
    "create a new shared collection when an existing user id is given" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val id = (for {
          _ ← deleteAllRows
          u ← createUser(userData)
          c ← collectionPersistenceServices.add[Long](
            collectionData.copy(userId = Option(u))
          )
        } yield c).transactAndRun

        val storedCollection = collectionPersistenceServices.getById(
          id = id
        ).transactAndRun

        storedCollection must beSome[SharedCollection].which {
          collection ⇒ collection.publicIdentifier must_== collectionData.publicIdentifier
        }
      }
    }
    "create a new shared collection without a defined user id" in {
      prop { (collectionData: SharedCollectionData) ⇒
        WithEmptyDatabase {
          val id: Long = collectionPersistenceServices.add[Long](
            collectionData.copy(userId = None)
          ).transactAndRun

          val storedCollection = collectionPersistenceServices.getById(
            id = id
          ).transactAndRun

          storedCollection must beSome[SharedCollection].which {
            collection ⇒ collection.publicIdentifier must_== collectionData.publicIdentifier
          }
        }
      }
    }
  }

  "getById" should {
    "return None if the table is empty" in {
      prop { (id: Long) ⇒
        WithEmptyDatabase {
          val collection = collectionPersistenceServices.getById(
            id = id
          ).transactAndRun

          collection must beNone
        }
      }
    }
    "return a collection if there is a record with the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val id = (for {
          _ ← deleteAllRows
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transactAndRun

        val storedCollection = collectionPersistenceServices.getById(
          id = id
        ).transactAndRun

        storedCollection must beSome[SharedCollection].which {
          collection ⇒ collection.publicIdentifier must_== collectionData.publicIdentifier
        }
      }
    }
    "return None if there isn't any collection with the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val id = (for {
          _ ← deleteAllRows
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transactAndRun

        val storedCollection = collectionPersistenceServices.getById(
          id = id + 1000000
        ).transactAndRun

        storedCollection must beNone
      }
    }
  }

  "getByPublicIdentifier" should {
    "return None if the table is empty" in {
      prop { (publicIdentifier: String) ⇒
        WithEmptyDatabase {
          val collection = collectionPersistenceServices.getByPublicIdentifier(
            publicIdentifier = publicIdentifier
          ).transactAndRun

          collection must beNone
        }
      }
    }
    "return a collection if there is a record with the given public identifier in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val id = (for {
          _ ← deleteAllRows
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transactAndRun

        val storedCollection = collectionPersistenceServices.getByPublicIdentifier(
          publicIdentifier = collectionData.publicIdentifier
        ).transactAndRun

        storedCollection must beSome[SharedCollection].which {
          collection ⇒
            collection.id must_== id
            collection.publicIdentifier must_== collectionData.publicIdentifier
        }
      }
    }
    "return None if there isn't any collection with the given public identifier in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val id = (for {
          _ ← deleteAllRows
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transactAndRun

        val collection = collectionPersistenceServices.getByPublicIdentifier(
          publicIdentifier = collectionData.publicIdentifier.reverse
        ).transactAndRun

        collection must beNone
      }
    }
  }

  "getByUserId" should {
    "return the List of Collections created by the User" in {
      prop { (ownerData: UserData, otherData: UserData, collectionData: List[SharedCollectionData]) ⇒
        val List(ownedData, disownedData, foreignData) = divideList[SharedCollectionData](3, collectionData)

        val setupTrans = for {
          _ ← deleteAllRows
          ownerId ← createUser(ownerData)
          otherId ← createUser(otherData)
          owned ← ownedData traverse (createCollectionWithUser(_, Option(ownerId)))
          foreign ← foreignData traverse (createCollectionWithUser(_, Option(otherId)))
          disowned ← disownedData traverse (createCollectionWithUser(_, None))
        } yield (ownerId, owned)

        val (ownerId, owned) = setupTrans.transactAndRun

        val response: List[SharedCollectionWithAggregatedInfo] =
          collectionPersistenceServices
            .getByUser(ownerId)
            .transactAndRun

        (response map (_.sharedCollectionData.id)) must containTheSameElementsAs(owned)
      }
    }
  }

  "getLatestByCategory" should {
    "return an empty list of collections if the tabls is empty" in {
      prop { category: String ⇒
        WithEmptyDatabase {
          val response: List[SharedCollection] =
            collectionPersistenceServices
              .getLatestByCategory(category, pageParams)
              .transactAndRun

          response must beEmpty
        }
      }
    }
    "return an empty list of collections if there are no records with the given category" in {
      prop { (userData: UserData, collectionsData: List[SharedCollectionData]) ⇒

        val setupTrans = for {
          _ ← deleteAllRows
          userId ← createUser(userData)
          collections ← createCollectionsWithCategoryAndUser(
            collectionsData = collectionsData,
            category        = socialCategory,
            userId          = Option(userId)
          )
        } yield (userId, collections)

        val (userId, collections) = setupTrans.transactAndRun

        val response: List[SharedCollection] =
          collectionPersistenceServices
            .getLatestByCategory(communicationCategory, pageParams)
            .transactAndRun

        response must beEmpty
      }
    }
    "return a list of latest collections for the given category" in {
      prop { (userData: UserData, collectionsData: List[SharedCollectionData]) ⇒

        val List(socialCollections, otherCollections) = divideList(2, collectionsData)

        val setupTrans = for {
          _ ← deleteAllRows
          userId ← createUser(userData)
          socialCollections ← createCollectionsWithCategoryAndUser(
            collectionsData = socialCollections,
            category        = socialCategory,
            userId          = Option(userId)
          )
          otherCollections ← createCollectionsWithCategoryAndUser(
            collectionsData = otherCollections,
            category        = communicationCategory,
            userId          = Option(userId)
          )
        } yield Unit

        setupTrans.transactAndRun

        val response = for {
          response ← collectionPersistenceServices.getLatestByCategory(
            category   = socialCategory,
            pageParams = pageParams
          )
          _ ← deleteSharedCollections
        } yield response

        val collections: List[SharedCollection] = response.transactAndRun

        val sortedSocialCollections = socialCollections.sortWith(_.publishedOn.getTime > _.publishedOn.getTime)

        collections.size must be_<=(pageSize)
        collections.headOption.map(_.publicIdentifier) must_== sortedSocialCollections.headOption.map(_.publicIdentifier)
      }
    }
  }

  "getTopByCategory" should {
    "return an empty list of collections if the table is empty" in {
      prop { i: Int ⇒
        WithEmptyDatabase {
          val response: List[SharedCollection] =
            collectionPersistenceServices
              .getLatestByCategory(socialCategory, pageParams)
              .transactAndRun

          response must beEmpty
        }
      }
    }
    "return an empty list of collections if there are no records with the given category" in {
      prop { (userData: UserData, collectionsData: List[SharedCollectionData]) ⇒

        val setupTrans = for {
          _ ← deleteAllRows
          userId ← createUser(userData)
          collections ← createCollectionsWithCategoryAndUser(collectionsData, socialCategory, Option(userId))
        } yield Unit

        setupTrans.transactAndRun

        val response: List[SharedCollection] =
          collectionPersistenceServices
            .getTopByCategory(communicationCategory, pageParams)
            .transactAndRun

        response must beEmpty
      }
    }
    "return a list of top collections for the given category" in {
      prop { (userData: UserData, collectionsData: List[SharedCollectionData]) ⇒

        val List(socialCollections, otherCollections) = divideList(2, collectionsData)

        val setupTrans = for {
          _ ← deleteAllRows
          userId ← createUser(userData)
          socialCollections ← createCollectionsWithCategoryAndUser(
            collectionsData = socialCollections,
            category        = socialCategory,
            userId          = Option(userId)
          )
          otherCollections ← createCollectionsWithCategoryAndUser(
            collectionsData = otherCollections,
            category        = communicationCategory,
            userId          = Option(userId)
          )
        } yield Unit

        setupTrans.transactAndRun

        val response = for {
          response ← collectionPersistenceServices.getTopByCategory(
            category   = socialCategory,
            pageParams = pageParams
          )
          _ ← deleteSharedCollections
        } yield response

        val collections: List[SharedCollection] = response.transactAndRun

        val maxInstallations =
          if (socialCollections.isEmpty)
            None
          else
            Option(socialCollections.map(_.installations).max)

        collections.size must be_<=(pageSize)
        collections.headOption.map(_.installations) must_== maxInstallations
      }
    }
  }

  "updateCollectionInfo" should {
    "return 0 updated rows if the table is empty" in {
      prop { (id: Long, title: String) ⇒
        WithEmptyDatabase {
          val updatedCollectionCount = collectionPersistenceServices.updateCollectionInfo(
            id    = id,
            title = title
          ).transactAndRun

          updatedCollectionCount must_== 0
        }
      }
    }
    "return 1 updated row if there is a collection with the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, newTitle: String) ⇒
        val id = (for {
          _ ← deleteAllRows
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          _ ← collectionPersistenceServices.updateCollectionInfo(c, newTitle)
        } yield c).transactAndRun

        val storedCollection = collectionPersistenceServices.getById(
          id = id
        ).transactAndRun

        storedCollection must beSome[SharedCollection].which {
          collection ⇒
            collection.name must_== newTitle
        }
      }
    }
    "return 0 updated rows if there isn't any collection with the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, newTitle: String) ⇒
        val id = (for {
          _ ← deleteAllRows
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transactAndRun

        val updatedCollectionCount = collectionPersistenceServices.updateCollectionInfo(
          id    = id + 1000000,
          title = newTitle
        ).transactAndRun

        updatedCollectionCount must_== 0
      }
    }
  }
}
