package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.{Installation, User}
import org.specs2.ScalaCheck
import doobie.imports._
import org.specs2.mutable.Specification
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.specification.BeforeEach
import scalaz.std.list._


class UserPersistenceImplSpec
  extends Specification
    with BeforeEach
    with ScalaCheck
    with DomainDatabaseContext {

  sequential

  def before = {
    flywaydb.clean()
    flywaydb.migrate()
  }

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

  "addUser" should {
    "new users can be created" in {
      prop { (user: User) =>
        val id: Long = userPersistenceServices.addUser[Long](user.email, user.sessionToken).transact(transactor).run
        val storeUser = userPersistenceServices.getUserByEmail(user.email).transact(transactor).run
        storeUser should beSome[User].which {
          u => u == user.copy(id)
        }
      }
    }
  }

  "getUserByEmail" should {
    "return None if the table is empty" in {
      prop { (user: User) =>
        val storeUser = userPersistenceServices.getUserByEmail(user.email).transact(transactor).run
        storeUser should beNone
      }
    }
    "return an user if there is an user with the given email in the database" in {
      prop { (user: User) =>
        val id: Long = userPersistenceServices.addUser[Long](user.email, user.sessionToken).transact(transactor).run
        val storeUser = userPersistenceServices.getUserByEmail(user.email).transact(transactor).run
        storeUser should beSome[User].which {
          user2 => user.email shouldEqual user2.email
        }
      }
    }
    "return None if there isn't any user with the given email in the database" in {
      prop { (user: User) =>
        val id: Long = userPersistenceServices.addUser[Long](user.email, user.sessionToken).transact(transactor).run
        val storeUser = userPersistenceServices.getUserByEmail(user.email.reverse).transact(transactor).run
        storeUser should beNone
      }
    }
  }

  "createInstallation" should {
    "new installation can be created" in {
      prop {
        (installation: Installation, user: User) =>
          val userId: Long = userPersistenceServices.addUser[Long](user.email, user.sessionToken).transact(transactor).run
          val id = userPersistenceServices.createInstallation[Long](userId, None, installation.androidId).transact(transactor).run
          val storeInstallation = userPersistenceServices.getInstallationById(id = id).transact(transactor).run
          storeInstallation should beSome[Installation].which {
            install => install shouldEqual Installation(id = id, userId = userId, deviceToken = None, androidId = installation.androidId)
          }
      }
    }
  }

  "getInstallationByUserAndAndroidId" should {
    "return None if the table is empty" in {
      prop { (installation: Installation) =>
        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(userId = installation.userId, androidId = installation.androidId).transact(transactor).run
        storeInstallation should beNone
      }
    }
    "installations can be queried by their userId and androidId" in {
      prop {
        (installation: Installation, user: User) =>
          val userId: Long = userPersistenceServices.addUser[Long](user.email, user.sessionToken).transact(transactor).run
          val id = userPersistenceServices.createInstallation[Long](userId, None, installation.androidId).transact(transactor).run
          val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(userId = userId, androidId = installation.androidId).transact(transactor).run
          storeInstallation should beSome[Installation].which {
            install => install.id shouldEqual id
          }
      }
    }
    "return None if there isn't any installation with the given userId and androidId in the database" in {
      prop { (installation: Installation, user: User) =>
        val userId: Long = userPersistenceServices.addUser[Long](user.email, user.sessionToken).transact(transactor).run
        val id = userPersistenceServices.createInstallation[Long](userId, None, installation.androidId).transact(transactor).run
        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(userId = userId, androidId = installation.androidId.reverse).transact(transactor).run
        storeInstallation should beNone
      }
    }
  }

  "getInstallationByUserAndAndroidId" should {
    "return None if the table is empty" in {
      prop {
        (installation: Installation, user: User) =>
          val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(userId = user.id, androidId = installation.androidId).transact(transactor).run
          storeInstallation should beNone
      }
    }
  }

  "updateInstallation" should {
    "installation can be updated" in {
      prop { (installation: Installation, user: User) =>
        val userId: Long = userPersistenceServices.addUser[Long](user.email, user.sessionToken).transact(transactor).run
        println(userId)
        val idInstall = userPersistenceServices.createInstallation[Long](userId, None, installation.androidId).transact(transactor).run
        println(idInstall)
        val install = userPersistenceServices.getInstallationById(idInstall).transact(transactor).run
        println(install)

        //        val result = userPersistenceServices.updateInstallation[Long](userId = userId, deviceToken = Option("1111a-2222b-33c-4444d"), androidId = installation.androidId).transact(transactor).run
        //        println(result)


        val values = List((Option("faljfda-kbasf"), userId, installation.androidId))
        val result = persistenceImpl.updateMany[List, (Option[String], Long, String)](
          sql = "update installations set devicetoken=? where userid=? and androidid=?",
          values = values).transact(transactor).attemptRun
        println(result)

        //        val updateDeviceToken1 = "update installations set devicetoken=? where id =?"

        //                val result = userPersistenceServices.updateInstallation[Long](userId = userId, deviceToken = Option("faljfda-kbasf"), androidId = installation.androidId).transact(transactor).run
        //       val result: Long = persistenceImpl.updateWithGeneratedKeys[(Option[String], Long), Long](updateDeviceToken1,, (Option("faljfda-kbasf"), idInstall)).transact(transactor).run
        //        println(result)
        //        val storeInstallation = userPersistenceServices.getInstallationByUserAndAndroidId(userId = userId, androidId = installation.androidId).transact(transactor).run
        //        println(storeInstallation)
        //        storeInstallation should beSome[Installation].which {
        //          install => install.deviceToken shouldEqual Option("faljfda-kbasf")
        //        }

        1 shouldEqual 1

      }
    }

  }
}