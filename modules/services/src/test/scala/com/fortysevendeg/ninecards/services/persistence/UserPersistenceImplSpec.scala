package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.{Installation, User}
import org.specs2.ScalaCheck
import doobie.imports._
import org.specs2.mutable.Specification
import org.scalacheck.{Arbitrary, Gen}


class UserPersistenceImplSpec
  extends Specification
    with ScalaCheck
    with DomainDatabaseContext {

  val userGenerator: Gen[User] = for {
    id <- Gen.resultOf((s: Long) => (s))
    email <- Gen.uuid
    sessionToken <- Gen.uuid
  } yield User(id = id, email = email.toString, sessionToken = sessionToken.toString, banned = false)

  implicit val arbUser: Arbitrary[User] = Arbitrary(userGenerator)

  val installationGenerator: Gen[Installation] = for {
    id <- Gen.resultOf((s: Long) => (s))
    userId <- Gen.resultOf((s: Long) => (s))
    androidId <- Gen.uuid
  } yield Installation(id = id, userId = userId, deviceToken = None, androidId = androidId.toString)

  implicit val arbInstallation: Arbitrary[Installation] = Arbitrary(installationGenerator)

  "getUserByEmail" should {
    "return a User if there is an element in the table that meets the criteria" in {
      prop { (user: User) =>
        val id: Long = userPersistenceServices.addUser[Long](user.email, user.sessionToken).transact(transactor).run
        val storeUser = userPersistenceServices.getUserByEmail(user.email).transact(transactor).run
        storeUser should beSome[User].which {
          user2 => user.email shouldEqual user2.email
        }

      }
    }
  }

  "getInstallationById" should {
    "return a Installation if there is an element in the table that meets the criteria" in {
      prop { (installation: Installation, user: User) =>
        val userId: Long = userPersistenceServices.addUser[Long](user.email, user.sessionToken).transact(transactor).run
        val id = userPersistenceServices.createInstallation[Long](userId, None, installation.androidId).transact(transactor).run
        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(userId = userId, androidId = installation.androidId).transact(transactor).run
        storeInstallation should beSome[Installation].which {
          install => install.id shouldEqual id
        }
      }
    }
  }
}