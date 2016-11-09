package cards.nine.services.free.interpreter.user

import cards.nine.domain.account._
import cards.nine.domain.ScalaCheck._
import cards.nine.services.free.domain.{ Installation, SharedCollection, SharedCollectionSubscription, User }
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import cards.nine.services.free.interpreter.user.Services.UserData
import cards.nine.services.persistence.NineCardsGenEntities._
import cards.nine.services.persistence.{ DomainDatabaseContext, NineCardsScalacheckGen }
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import shapeless.syntax.std.product._

class ServicesSpec
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

  def generateSubscribedInstallation(
    userData: UserData,
    collectionData: SharedCollectionData,
    androidId: AndroidId,
    deviceToken: DeviceToken
  ) = {
    for {
      u ← insertItem(User.Queries.insert, userData.toTuple)
      i ← insertItem(Installation.Queries.insert, (u, Option(deviceToken.value), androidId.value))
      c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
      _ ← insertItemWithoutGeneratedKeys(
        sql    = SharedCollectionSubscription.Queries.insert,
        values = (c, u, collectionData.publicIdentifier)
      )
    } yield i
  }.transactAndRun

  "addUser" should {
    "new users can be created" in {
      prop { (apiKey: ApiKey, email: Email, sessionToken: SessionToken) ⇒
        val id: Long = userPersistenceServices.addUser[Long](
          email        = email,
          apiKey       = apiKey,
          sessionToken = sessionToken
        ).transactAndRun

        val storeUser = userPersistenceServices.getUserByEmail(email).transactAndRun

        storeUser should beSome[User].which {
          user ⇒ user.email shouldEqual email
        }
      }
    }
  }

  "getUserByEmail" should {
    "return None if the table is empty" in {
      prop { (email: Email) ⇒
        val storeUser = userPersistenceServices.getUserByEmail(email).transactAndRun
        storeUser should beNone
      }
    }
    "return an user if there is an user with the given email in the database" in {
      prop { (apiKey: ApiKey, email: Email, sessionToken: SessionToken) ⇒
        val id: Long = userPersistenceServices.addUser[Long](
          email        = email,
          apiKey       = apiKey,
          sessionToken = sessionToken
        ).transactAndRun
        val storeUser = userPersistenceServices.getUserByEmail(email).transactAndRun

        storeUser should beSome[User].which {
          user ⇒
            val expectedUser = User(
              id           = id,
              email        = email,
              apiKey       = apiKey,
              sessionToken = sessionToken,
              banned       = false
            )

            user shouldEqual expectedUser
        }
      }
    }
    "return None if there isn't any user with the given email in the database" in {
      prop { (email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒
        val id: Long = userPersistenceServices.addUser[Long](
          email        = email,
          apiKey       = apiKey,
          sessionToken = sessionToken
        ).transactAndRun
        val storeUser = userPersistenceServices.getUserByEmail(Email(email.value.reverse)).transactAndRun

        storeUser should beNone
      }
    }
  }

  "getUserBySessionToken" should {
    "return None if the table is empty" in {
      prop { (email: Email, sessionToken: SessionToken) ⇒

        val user = userPersistenceServices.getUserBySessionToken(
          sessionToken = sessionToken
        ).transactAndRun

        user should beNone
      }
    }
    "return an user if there is an user with the given sessionToken in the database" in {
      prop { (email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒
        insertItem(
          sql    = User.Queries.insert,
          values = (email.value, sessionToken.value, apiKey.value)
        ).transactAndRun

        val user = userPersistenceServices.getUserBySessionToken(
          sessionToken = sessionToken
        ).transactAndRun

        user should beSome[User]
      }
    }
    "return None if there isn't any user with the given sessionToken in the database" in {
      prop { (email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒
        insertItem(
          sql    = User.Queries.insert,
          values = (email.value, sessionToken.value, apiKey.value)
        ).transactAndRun

        val user = userPersistenceServices.getUserBySessionToken(
          sessionToken = SessionToken(sessionToken.value.reverse)
        ).transactAndRun

        user should beNone
      }
    }
  }

  "createInstallation" should {
    "new installation can be created" in {
      prop { (androidId: AndroidId, email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒
        val userId = insertItem(
          sql    = User.Queries.insert,
          values = (email.value, sessionToken.value, apiKey.value)
        ).transactAndRun

        userPersistenceServices.createInstallation[Long](
          userId      = userId,
          deviceToken = None,
          androidId   = androidId
        ).transactAndRun

        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(
          userId    = userId,
          androidId = androidId
        ).transactAndRun

        storeInstallation should beSome[Installation].which { install ⇒
          install.userId must_== userId
          install.deviceToken must_== None
          install.androidId must_== androidId
        }
      }
    }
  }

  "getInstallationByUserAndAndroidId" should {
    "return None if the table is empty" in {
      prop { (androidId: AndroidId, userId: Long) ⇒
        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(
          userId    = userId,
          androidId = androidId
        ).transactAndRun

        storeInstallation should beNone
      }
    }
    "installations can be queried by their userId and androidId" in {
      prop { (androidId: AndroidId, email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒
        val userId = insertItem(
          sql    = User.Queries.insert,
          values = (email.value, sessionToken.value, apiKey.value)
        ).transactAndRun

        val id = insertItem(
          sql    = Installation.Queries.insert,
          values = (userId, emptyDeviceToken, androidId.value)
        ).transactAndRun

        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(
          userId    = userId,
          androidId = androidId
        ).transactAndRun

        storeInstallation should beSome[Installation].which {
          install ⇒ install.id shouldEqual id
        }
      }
    }
    "return None if there isn't any installation with the given userId and androidId in the database" in {
      prop { (androidId: AndroidId, email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒
        val userId = insertItem(
          sql    = User.Queries.insert,
          values = (email.value, sessionToken.value, apiKey.value)
        ).transactAndRun
        val id = insertItem(
          sql    = Installation.Queries.insert,
          values = (userId, emptyDeviceToken, androidId.value)
        ).transactAndRun

        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(
          userId    = userId,
          androidId = AndroidId(androidId.value.reverse)
        ).transactAndRun

        storeInstallation should beNone
      }
    }
  }

  "getSubscribedInstallationByCollection" should {
    "return an empty list if the table is empty" in {
      prop { (publicIdentifier: PublicIdentifier) ⇒
        val storeInstallation = userPersistenceServices.getSubscribedInstallationByCollection(
          publicIdentifier = publicIdentifier.value
        ).transactAndRun

        storeInstallation must beEmpty
      }
    }
    "return a list of installations that are subscribed to the collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, androidId: AndroidId, deviceToken: DeviceToken) ⇒
        generateSubscribedInstallation(userData, collectionData, androidId, deviceToken)

        val storeInstallation = userPersistenceServices.getSubscribedInstallationByCollection(
          publicIdentifier = collectionData.publicIdentifier
        ).transactAndRun

        storeInstallation must haveSize(be_>(0))
      }
    }
    "return an empty list if there is no installation subscribed to the collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, androidId: AndroidId, deviceToken: DeviceToken) ⇒
        generateSubscribedInstallation(userData, collectionData, androidId, deviceToken)

        val storeInstallation = userPersistenceServices.getSubscribedInstallationByCollection(
          publicIdentifier = collectionData.publicIdentifier.reverse
        ).transactAndRun

        storeInstallation must beEmpty
      }
    }
  }

}
