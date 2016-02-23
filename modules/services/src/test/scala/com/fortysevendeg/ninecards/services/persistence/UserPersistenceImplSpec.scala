package com.fortysevendeg.ninecards.services.persistence


import com.fortysevendeg.ninecards.services.free.domain.{Installation, User}
import com.fortysevendeg.ninecards.services.persistence.NineCardsGenEntities.{AndroidId, SessionToken, Email}
import org.specs2.ScalaCheck
import doobie.imports._
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.specification.BeforeEach
import org.specs2.mutable.Specification
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.specification.BeforeEach
import org.scalacheck.Arbitrary.arbitrary
import scalaz.std.list._


class UserPersistenceImplSpec
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
      prop { (email: Email, sessionToken: SessionToken) =>
        val id: Long = userPersistenceServices.addUser[Long](email.value, sessionToken.value).transact(transactor).run
        val storeUser = userPersistenceServices.getUserByEmail(email.value).transact(transactor).run
        storeUser should beSome[User].which {
          user => user.email shouldEqual email.value
        }
      }
    }
  }

  "getUserByEmail" should {
    "return None if the table is empty" in {
      prop { (email: Email) =>
        val storeUser = userPersistenceServices.getUserByEmail(email.value).transact(transactor).run
        storeUser should beNone
      }
    }
    "return an user if there is an user with the given email in the database" in {
      prop { (email: Email, sessionToken: SessionToken) =>
        val id: Long = userPersistenceServices.addUser[Long](email.value, sessionToken.value).transact(transactor).run
        val storeUser = userPersistenceServices.getUserByEmail(email.value).transact(transactor).run
        storeUser should beSome[User].which {
          user => user shouldEqual User(id = id, email = email.value, sessionToken = sessionToken.value, banned = false)
        }
      }
    }
    "return None if there isn't any user with the given email in the database" in {
      prop { (email: Email, sessionToken: SessionToken) =>
        val id: Long = userPersistenceServices.addUser[Long](email.value, sessionToken.value).transact(transactor).run
        val storeUser = userPersistenceServices.getUserByEmail(email.value.reverse).transact(transactor).run
        storeUser should beNone
      }
    }
  }

  "createInstallation" should {
    "new installation can be created" in {
      prop { (androidId: AndroidId, email: Email, sessionToken: SessionToken) =>
        val userId: Long = userPersistenceServices.addUser[Long](email.value, sessionToken.value).transact(transactor).run
        val id = userPersistenceServices.createInstallation[Long](userId, None, androidId.value).transact(transactor).run
        val storeInstallation = userPersistenceServices.getInstallationById(id = id).transact(transactor).run
        storeInstallation should beSome[Installation].which {
          install => install shouldEqual Installation(id = id, userId = userId, deviceToken = None, androidId = androidId.value)
        }
      }
    }
  }

  "getInstallationByUserAndAndroidId" should {
    "return None if the table is empty" in {
      prop { (androidId: AndroidId, userId: Long) =>
        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(userId = userId, androidId = androidId.value).transact(transactor).run
        storeInstallation should beNone
      }
    }
    "installations can be queried by their userId and androidId" in {
      prop { (androidId: AndroidId, email: Email, sessionToken: SessionToken) =>
        val userId: Long = userPersistenceServices.addUser[Long](email.value, sessionToken.value).transact(transactor).run
        val id = userPersistenceServices.createInstallation[Long](userId, None, androidId.value).transact(transactor).run
        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(userId = userId, androidId = androidId.value).transact(transactor).run
        storeInstallation should beSome[Installation].which {
          install => install.id shouldEqual id
        }
      }
    }
    "return None if there isn't any installation with the given userId and androidId in the database" in {
      prop { (androidId: AndroidId, email: Email, sessionToken: SessionToken) =>
        val userId: Long = userPersistenceServices.addUser[Long](email.value, sessionToken.value).transact(transactor).run
        val id = userPersistenceServices.createInstallation[Long](userId, None, androidId.value).transact(transactor).run
        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(userId = userId, androidId = androidId.value.reverse).transact(transactor).run
        storeInstallation should beNone
      }
    }
  }
}