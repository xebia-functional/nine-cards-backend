package cards.nine.services.free.interpreter.user

import cards.nine.commons.NineCardsErrors._
import cards.nine.domain.account._
import cards.nine.domain.ScalaCheck._
import cards.nine.services.free.domain.{ Installation, SharedCollection, SharedCollectionSubscription, User }
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import cards.nine.services.free.interpreter.user.Services.UserData
import cards.nine.services.persistence.NineCardsGenEntities.PublicIdentifier
import cards.nine.services.persistence.{ DomainDatabaseContext, NineCardsScalacheckGen }
import doobie.contrib.postgresql.pgtypes._
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

  sequential

  object WithData {

    def apply[A](userData: UserData)(check: Long ⇒ MatchResult[A]) = {
      val id = {
        for {
          _ ← deleteAllRows
          id ← insertItem(User.Queries.insert, userData.toTuple)
        } yield id
      }.transactAndRun

      check(id)
    }

    def apply[A](userData: UserData, androidId: AndroidId)(check: (Long, Long) ⇒ MatchResult[A]) = {
      val (userId, installationId) = {
        for {
          _ ← deleteAllRows
          u ← insertItem(User.Queries.insert, userData.toTuple)
          i ← insertItem(Installation.Queries.insert, (u, emptyDeviceToken, androidId.value))
        } yield (u, i)
      }.transactAndRun

      check(userId, installationId)
    }

    def apply[A](
      userData: UserData,
      androidId: AndroidId,
      deviceToken: DeviceToken,
      collectionData: SharedCollectionData
    )(check: ⇒ MatchResult[A]) = {
      {
        for {
          _ ← deleteAllRows
          u ← insertItem(User.Queries.insert, userData.toTuple)
          i ← insertItem(Installation.Queries.insert, (u, Option(deviceToken.value), androidId.value))
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          _ ← insertItemWithoutGeneratedKeys(
            sql    = SharedCollectionSubscription.Queries.insert,
            values = (c, u, collectionData.publicIdentifier)
          )
        } yield (u, i, c)
      }.transactAndRun

      check
    }
  }

  "addUser" should {
    "new users can be created" in {
      prop { userData: UserData ⇒
        WithEmptyDatabase {
          userPersistenceServices.addUser[Long](
            email = Email(userData.email),
            apiKey = ApiKey(userData.apiKey),
            sessionToken = SessionToken(userData.sessionToken)
          ).transactAndRun

          val user = getItem[String, User](User.Queries.getByEmail, userData.email).transactAndRun

          user must beLike {
            case user: User ⇒ user.email.value must_== userData.email
          }
        }
      }
    }
  }

  "getUserByEmail" should {
    "return an UserNotFound error if the table is empty" in {
      prop { (email: Email) ⇒
        WithEmptyDatabase {
          val user = userPersistenceServices.getUserByEmail(email).transactAndRun

          user should beLeft(UserNotFound(s"User with email ${email.value} not found"))
        }
      }
    }
    "return an user if there is an user with the given email in the database" in {
      prop { userData: UserData ⇒

        WithData(userData) { id ⇒
          val user = userPersistenceServices.getUserByEmail(Email(userData.email)).transactAndRun

          user should beRight[User].which {
            user ⇒
              user.id must_== id
              user.apiKey.value must_== userData.apiKey
              user.email.value must_== userData.email
              user.sessionToken.value must_== userData.sessionToken
          }
        }
      }
    }
    "return an UserNotFound error if there isn't any user with the given email in the database" in {
      prop { userData: UserData ⇒

        WithData(userData) { id ⇒
          val wrongEmail = Email(userData.email.reverse)

          val user = userPersistenceServices.getUserByEmail(wrongEmail).transactAndRun

          user should beLeft(UserNotFound(s"User with email ${wrongEmail.value} not found"))
        }
      }
    }
  }

  "getUserBySessionToken" should {
    "return an UserNotFound error if the table is empty" in {
      prop { (email: Email, sessionToken: SessionToken) ⇒
        WithEmptyDatabase {
          val user = userPersistenceServices.getUserBySessionToken(
            sessionToken = sessionToken
          ).transactAndRun

          user should beLeft(UserNotFound(s"User with sessionToken ${sessionToken.value} not found"))
        }
      }
    }

    "return an user if there is an user with the given sessionToken in the database" in {
      prop { userData: UserData ⇒

        WithData(userData) { id ⇒

          val user = userPersistenceServices.getUserBySessionToken(
            sessionToken = SessionToken(userData.sessionToken)
          ).transactAndRun

          user should beRight[User].which {
            user ⇒
              user.id must_== id
              user.apiKey.value must_== userData.apiKey
              user.email.value must_== userData.email
              user.sessionToken.value must_== userData.sessionToken
          }
        }
      }
    }

    "return an UserNotFound error if there isn't any user with the given sessionToken in the database" in {
      prop { userData: UserData ⇒

        WithData(userData) { id ⇒
          val wrongSessionToken = SessionToken(userData.sessionToken.reverse)

          val user = userPersistenceServices.getUserBySessionToken(wrongSessionToken).transactAndRun

          user should beLeft(UserNotFound(s"User with sessionToken ${wrongSessionToken.value} not found"))
        }
      }
    }
  }

  "createInstallation" should {
    "new installation can be created" in {
      prop { (androidId: AndroidId, userData: UserData) ⇒

        WithData(userData) { userId ⇒
          userPersistenceServices.createInstallation[Long](
            userId      = userId,
            deviceToken = None,
            androidId   = androidId
          ).transactAndRun

          val installation = getItem[(Long, String), Installation](
            Installation.Queries.getByUserAndAndroidId,
            (userId, androidId.value)
          ).transactAndRun

          installation must beLike {
            case install: Installation ⇒
              install.userId must_== userId
              install.deviceToken must_== None
              install.androidId must_== androidId
          }
        }
      }
    }
  }

  "getInstallationByUserAndAndroidId" should {
    "return an InstallationNotFound error if the table is empty" in {
      prop { (androidId: AndroidId, userId: Long) ⇒
        WithEmptyDatabase {
          val installation = userPersistenceServices.getInstallationByUserAndAndroidId(
            userId = userId,
            androidId = androidId
          ).transactAndRun

        installation must beLeft(InstallationNotFound(s"Installation for android id ${androidId.value} not found"))
        }
      }
    }
    "installations can be queried by their userId and androidId" in {
      prop { (androidId: AndroidId, userData: UserData) ⇒

        WithData(userData, androidId) { (userId, installationId) ⇒

          val installation = userPersistenceServices.getInstallationByUserAndAndroidId(
            userId    = userId,
            androidId = androidId
          ).transactAndRun

          installation should beRight[Installation].which {
            install ⇒ install.id must_== installationId
          }
        }
      }
    }
    "return an InstallationNotFound error if there isn't any installation with the given userId " +
      "and androidId in the database" in {
        prop { (androidId: AndroidId, userData: UserData) ⇒

          WithData(userData, androidId) { (userId, installationId) ⇒

            val wrongAndroidId = AndroidId(androidId.value.reverse)

            val installation = userPersistenceServices.getInstallationByUserAndAndroidId(
              userId    = userId,
              androidId = wrongAndroidId
            ).transactAndRun

            installation must beLeft(InstallationNotFound(s"Installation for android id ${wrongAndroidId.value} not found"))
          }
        }
      }
  }

  "getSubscribedInstallationByCollection" should {
    "return an empty list if the table is empty" in {
      prop { (publicIdentifier: PublicIdentifier) ⇒
        WithEmptyDatabase {
          val installation = userPersistenceServices.getSubscribedInstallationByCollection(
            publicIdentifier = publicIdentifier.value
          ).transactAndRun

          installation must beRight[List[Installation]](Nil)
        }
      }
    }
    "return a list of installations that are subscribed to the collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, androidId: AndroidId, deviceToken: DeviceToken) ⇒

        WithData(userData, androidId, deviceToken, collectionData) {

          val installation = userPersistenceServices.getSubscribedInstallationByCollection(
            publicIdentifier = collectionData.publicIdentifier
          ).transactAndRun

          installation must beRight[List[Installation]].which { list ⇒
            list must haveSize(be_>(0))
          }
        }
      }
    }
    "return an empty list if there is no installation subscribed to the collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, androidId: AndroidId, deviceToken: DeviceToken) ⇒

        WithData(userData, androidId, deviceToken, collectionData) {

          val installation = userPersistenceServices.getSubscribedInstallationByCollection(
            publicIdentifier = collectionData.publicIdentifier.reverse
          ).transactAndRun

          installation must beRight[List[Installation]](Nil)
        }
      }
    }
  }

}
