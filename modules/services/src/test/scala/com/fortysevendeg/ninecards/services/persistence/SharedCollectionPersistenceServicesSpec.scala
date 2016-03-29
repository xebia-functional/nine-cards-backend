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

import scalaz.std.iterable._

class SharedCollectionPersistenceServicesSpec
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

  "addCollection" should {
    "create a new shared collection when an existing user id is given" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) =>
        val id = (for {
          u <- insertItem(User.Queries.insert, userData.toTuple)
          c <- sharedCollectionPersistenceServices.addCollection[Long](
            collectionData.copy(userId = Option(u)))
        } yield c).transact(transactor).run

        val storedCollection = sharedCollectionPersistenceServices.getCollectionById(
          id = id).transact(transactor).run

        storedCollection must beSome[SharedCollection].which {
          collection => collection.publicIdentifier must_== collectionData.publicIdentifier
        }
      }
    }

    "create a new shared collection without a defined user id" in {
      prop { (collectionData: SharedCollectionData) =>
        val id: Long = sharedCollectionPersistenceServices.addCollection[Long](
          collectionData.copy(userId = None)).transact(transactor).run

        val storedCollection = sharedCollectionPersistenceServices.getCollectionById(
          id = id).transact(transactor).run

        storedCollection must beSome[SharedCollection].which {
          collection => collection.publicIdentifier must_== collectionData.publicIdentifier
        }
      }
    }
  }

  "getCollectionById" should {
    "return None if the table is empty" in {
      prop { (id: Long) =>
        val collection = sharedCollectionPersistenceServices.getCollectionById(
          id = id).transact(transactor).run

        collection must beNone
      }
    }
    "return a collection if there is a record with the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) =>
        val id = (for {
          u <- insertItem(User.Queries.insert, userData.toTuple)
          c <- insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transact(transactor).run

        val storedCollection = sharedCollectionPersistenceServices.getCollectionById(
          id = id).transact(transactor).run

        storedCollection must beSome[SharedCollection].which {
          collection => collection.publicIdentifier must_== collectionData.publicIdentifier
        }
      }
    }
    "return None if there isn't any collection with the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) =>
        val id = (for {
          u <- insertItem(User.Queries.insert, userData.toTuple)
          c <- insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transact(transactor).run

        val storedCollection = sharedCollectionPersistenceServices.getCollectionById(
          id = id + 1000000).transact(transactor).run

        storedCollection must beNone
      }
    }
  }

  "getCollectionByPublicIdentifier" should {
    "return None if the table is empty" in {
      prop { (publicIdentifier: String) =>

        val collection = sharedCollectionPersistenceServices.getCollectionByPublicIdentifier(
          publicIdentifier = publicIdentifier).transact(transactor).run

        collection must beNone
      }
    }
    "return a collection if there is a record with the given public identifier in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) =>
        val id = (for {
          u <- insertItem(User.Queries.insert, userData.toTuple)
          c <- insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transact(transactor).run

        val storedCollection = sharedCollectionPersistenceServices.getCollectionByPublicIdentifier(
          publicIdentifier = collectionData.publicIdentifier).transact(transactor).run

        storedCollection must beSome[SharedCollection].which {
          collection =>
            collection.id must_== id
            collection.publicIdentifier must_== collectionData.publicIdentifier
        }
      }
    }
    "return None if there isn't any collection with the given public identifier in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) =>
        val id = (for {
          u <- insertItem(User.Queries.insert, userData.toTuple)
          c <- insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transact(transactor).run

        val collection = sharedCollectionPersistenceServices.getCollectionByPublicIdentifier(
          publicIdentifier = collectionData.publicIdentifier.reverse).transact(transactor).run

        collection must beNone
      }
    }
  }

  "addPackage" should {
    "create a new package associated with an existing shared collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, packageName: String) =>
        val collectionId = (for {
          u <- insertItem(User.Queries.insert, userData.toTuple)
          c <- insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transact(transactor).run

        val packageId = sharedCollectionPersistenceServices.addPackage[Long](
          collectionId,
          packageName).transact(transactor).run

        val storedPackages = sharedCollectionPersistenceServices.getPackagesByCollection(
          collectionId).transact(transactor).run

        storedPackages must contain { p: SharedCollectionPackage =>
          p.id must_== packageId
        }.atMostOnce
      }
    }
  }

  "addPackages" should {
    "create new packages associated with an existing shared collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, packagesName: List[String]) =>
        val collectionId = (for {
          u <- insertItem(User.Queries.insert, userData.toTuple)
          c <- insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transact(transactor).run

        val created = sharedCollectionPersistenceServices.addPackages(
          collectionId,
          packagesName).transact(transactor).run

        created must_== packagesName.size
      }
    }
  }

  "getPackagesByCollection" should {
    "return an empty list if the table is empty" in {
      prop { (collectionId: Long) =>
        val packages = sharedCollectionPersistenceServices.getPackagesByCollection(
          collectionId).transact(transactor).run

        packages must beEmpty
      }
    }
    "return a list of packages associated with the given shared collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, packagesName: List[String]) =>
        val collectionId = (for {
          u <- insertItem(User.Queries.insert, userData.toTuple)
          c <- insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          _ <- insertItems(SharedCollectionPackage.Queries.insert, packagesName map { (c, _) })
        } yield c).transact(transactor).run

        val packages = sharedCollectionPersistenceServices.getPackagesByCollection(
          collectionId).transact(transactor).run

        packages must haveSize(packagesName.size)

        packages must contain { p: SharedCollectionPackage =>
          p.sharedCollectionId must_=== collectionId
        }.forall
      }
    }
    "return an empty list if there isn't any package associated with the given collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, packagesName: List[String]) =>
        val collectionId = (for {
          u <- insertItem(User.Queries.insert, userData.toTuple)
          c <- insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          _ <- insertItems(SharedCollectionPackage.Queries.insert, packagesName map { (c, _) })
        } yield c).transact(transactor).run

        val packages = sharedCollectionPersistenceServices.getPackagesByCollection(
          collectionId + 1000000).transact(transactor).run

        packages must beEmpty
      }
    }
  }
}