package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.{Installation, User}
import com.fortysevendeg.ninecards.services.persistence.NineCardsGenEntities._
import doobie.imports._
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach

class UserPersistenceServicesSpec
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

  "addUser" should {
    "new users can be created" in {
      prop { (apiKey: ApiKey, email: Email, sessionToken: SessionToken) ⇒
        val id: Long = userPersistenceServices.addUser[Long](
          email        = email.value,
          apiKey       = apiKey.value,
          sessionToken = sessionToken.value
        ).transact(transactor).run

        val storeUser = userPersistenceServices.getUserByEmail(email.value).transact(transactor).run

        storeUser should beSome[User].which {
          user ⇒ user.email shouldEqual email.value
        }
      }
    }
  }

  "getUserByEmail" should {
    "return None if the table is empty" in {
      prop { (email: Email) ⇒
        val storeUser = userPersistenceServices.getUserByEmail(email.value).transact(transactor).run
        storeUser should beNone
      }
    }
    "return an user if there is an user with the given email in the database" in {
      prop { (apiKey: ApiKey, email: Email, sessionToken: SessionToken) ⇒
        val id: Long = userPersistenceServices.addUser[Long](
          email        = email.value,
          apiKey       = apiKey.value,
          sessionToken = sessionToken.value
        ).transact(transactor).run
        val storeUser = userPersistenceServices.getUserByEmail(email.value).transact(transactor).run

        storeUser should beSome[User].which {
          user ⇒
            val expectedUser = User(
              id           = id,
              email        = email.value,
              apiKey       = apiKey.value,
              sessionToken = sessionToken.value,
              banned       = false
            )

            user shouldEqual expectedUser
        }
      }
    }
    "return None if there isn't any user with the given email in the database" in {
      prop { (email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒
        val id: Long = userPersistenceServices.addUser[Long](
          email        = email.value,
          apiKey       = apiKey.value,
          sessionToken = sessionToken.value
        ).transact(transactor).run
        val storeUser = userPersistenceServices.getUserByEmail(email.value.reverse).transact(transactor).run

        storeUser should beNone
      }
    }
  }

  "getUserBySessionToken" should {
    "return None if the table is empty" in {
      prop { (email: Email, sessionToken: SessionToken) ⇒

        val user = userPersistenceServices.getUserBySessionToken(
          sessionToken = sessionToken.value
        ).transact(transactor).run

        user should beNone
      }
    }
    "return an user if there is an user with the given sessionToken in the database" in {
      prop { (email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒
        insertItem(
          sql    = User.Queries.insert,
          values = (email.value, sessionToken.value, apiKey.value)
        ).transact(transactor).run

        val user = userPersistenceServices.getUserBySessionToken(
          sessionToken = sessionToken.value
        ).transact(transactor).run

        user should beSome[User]
      }
    }
    "return None if there isn't any user with the given sessionToken in the database" in {
      prop { (email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒
        insertItem(
          sql    = User.Queries.insert,
          values = (email.value, sessionToken.value, apiKey.value)
        ).transact(transactor).run

        val user = userPersistenceServices.getUserBySessionToken(
          sessionToken = sessionToken.value.reverse
        ).transact(transactor).run

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
        ).transact(transactor).run

        val id = userPersistenceServices.createInstallation[Long](
          userId      = userId,
          deviceToken = None,
          androidId   = androidId.value
        ).transact(transactor).run

        val storeInstallation = userPersistenceServices.getInstallationById(
          id = id
        ).transact(transactor).run

        storeInstallation should beSome[Installation].which {
          install ⇒ install shouldEqual Installation(id = id, userId = userId, deviceToken = None, androidId = androidId.value)
        }
      }
    }
  }

  "getInstallationByUserAndAndroidId" should {
    "return None if the table is empty" in {
      prop { (androidId: AndroidId, userId: Long) ⇒
        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(
          userId    = userId,
          androidId = androidId.value
        ).transact(transactor).run

        storeInstallation should beNone
      }
    }
    "installations can be queried by their userId and androidId" in {
      prop { (androidId: AndroidId, email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒
        val userId = insertItem(
          sql    = User.Queries.insert,
          values = (email.value, sessionToken.value, apiKey.value)
        ).transact(transactor).run

        val id = insertItem(
          sql    = Installation.Queries.insert,
          values = (userId, emptyDeviceToken, androidId.value)
        ).transact(transactor).run

        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(
          userId    = userId,
          androidId = androidId.value
        ).transact(transactor).run

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
        ).transact(transactor).run
        val id = insertItem(
          sql    = Installation.Queries.insert,
          values = (userId, emptyDeviceToken, androidId.value)
        ).transact(transactor).run

        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(
          userId    = userId,
          androidId = androidId.value.reverse
        ).transact(transactor).run

        storeInstallation should beNone
      }
    }
  }
}