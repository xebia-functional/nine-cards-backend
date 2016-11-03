package cards.nine.services.free.interpreter.user

import cards.nine.commons.NineCardsErrors.{ InstallationNotFound, NineCardsError, UserNotFound }
import cards.nine.domain.account._
import cards.nine.domain.ScalaCheck._
import cards.nine.services.free.domain.{ Installation, SharedCollection, SharedCollectionSubscription, User }
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import cards.nine.services.persistence.NineCardsGenEntities.PublicIdentifier
import cards.nine.services.persistence.{ DomainDatabaseContext, NineCardsScalacheckGen }
import org.specs2.ScalaCheck
import org.specs2.matcher.{ DisjunctionMatchers, MatchResult }
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

  object WithData {

    def apply[A](apiKey: ApiKey, email: Email, sessionToken: SessionToken)(check: Long ⇒ MatchResult[A]) = {
      val id = insertItem(User.Queries.insert, (email.value, sessionToken.value, apiKey.value)).transactAndRun

      check(id)
    }

    def apply[A](
      apiKey: ApiKey,
      email: Email,
      sessionToken: SessionToken,
      androidId: AndroidId
    )(check: (Long, Long) ⇒ MatchResult[A]) = {
      val (userId, installationId) = {
        for {
          u ← insertItem(User.Queries.insert, (email.value, sessionToken.value, apiKey.value))
          i ← insertItem(Installation.Queries.insert, (u, emptyDeviceToken, androidId.value))
        } yield (u, i)
      }.transactAndRun

      check(userId, installationId)
    }

    def apply[A](
      apiKey: ApiKey,
      email: Email,
      sessionToken: SessionToken,
      androidId: AndroidId,
      deviceToken: DeviceToken,
      collectionData: SharedCollectionData
    )(check: ⇒ MatchResult[A]) = {
      {
        for {
          u ← insertItem(User.Queries.insert, (email.value, sessionToken.value, apiKey.value))
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
      prop { (apiKey: ApiKey, email: Email, sessionToken: SessionToken) ⇒
        userPersistenceServices.addUser[Long](
          email        = email,
          apiKey       = apiKey,
          sessionToken = sessionToken
        ).transactAndRun

        val user = getItem[String, User](User.Queries.getByEmail, email.value).transactAndRun

        user should beLike {
          case user: User ⇒ user.email shouldEqual email
        }
      }
    }
  }

  "getUserByEmail" should {
    "return an UserNotFound error if the table is empty" in {
      prop { (email: Email) ⇒
        val user = userPersistenceServices.getUserByEmail(email).transactAndRun

        user should beLeft[NineCardsError].which {
          error ⇒
            error must_== UserNotFound(s"User with email ${email.value} not found")
        }
      }
    }
    "return an user if there is an user with the given email in the database" in {
      prop { (apiKey: ApiKey, email: Email, sessionToken: SessionToken) ⇒

        WithData(apiKey, email, sessionToken) { id ⇒
          val user = userPersistenceServices.getUserByEmail(email).transactAndRun

          user should beRight[User].which {
            user ⇒
              user.id must_== id
              user.apiKey must_== apiKey
              user.email must_== email
              user.sessionToken must_== sessionToken
          }
        }
      }
    }
    "return an UserNotFound error if there isn't any user with the given email in the database" in {
      prop { (email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒

        WithData(apiKey, email, sessionToken) { id ⇒
          val wrongEmail = Email(email.value.reverse)

          val user = userPersistenceServices.getUserByEmail(wrongEmail).transactAndRun

          user should beLeft[NineCardsError].which {
            error ⇒
              error must_== UserNotFound(s"User with email ${wrongEmail.value} not found")
          }
        }
      }
    }
  }

  "getUserBySessionToken" should {
    "return an UserNotFound error if the table is empty" in {
      prop { (email: Email, sessionToken: SessionToken) ⇒

        val user = userPersistenceServices.getUserBySessionToken(
          sessionToken = sessionToken
        ).transactAndRun

        user should beLeft[NineCardsError].which {
          error ⇒
            error must_== UserNotFound(s"User with sessionToken ${sessionToken.value} not found")
        }
      }
    }

    "return an user if there is an user with the given sessionToken in the database" in {
      prop { (email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒

        WithData(apiKey, email, sessionToken) { id ⇒

          val user = userPersistenceServices.getUserBySessionToken(
            sessionToken = sessionToken
          ).transactAndRun

          user should beRight[User].which {
            user ⇒
              user.id must_== id
              user.apiKey must_== apiKey
              user.email must_== email
              user.sessionToken must_== sessionToken
          }
        }
      }
    }

    "return an UserNotFound error if there isn't any user with the given sessionToken in the database" in {
      prop { (email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒

        WithData(apiKey, email, sessionToken) { id ⇒
          val wrongSessionToken = SessionToken(sessionToken.value.reverse)

          val user = userPersistenceServices.getUserBySessionToken(wrongSessionToken).transactAndRun

          user should beLeft[NineCardsError].which {
            error ⇒
              error must_== UserNotFound(s"User with sessionToken ${wrongSessionToken.value} not found")
          }
        }
      }
    }
  }

  "createInstallation" should {
    "new installation can be created" in {
      prop { (androidId: AndroidId, email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒

        WithData(apiKey, email, sessionToken) { userId ⇒
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

        val installation = userPersistenceServices.getInstallationByUserAndAndroidId(
          userId    = userId,
          androidId = androidId
        ).transactAndRun

        installation must beLeft[NineCardsError].which { error ⇒
          error must_== InstallationNotFound(s"Installation for android id ${androidId.value} not found")
        }
      }
    }
    "installations can be queried by their userId and androidId" in {
      prop { (androidId: AndroidId, email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒

        WithData(apiKey, email, sessionToken, androidId) { (userId, installationId) ⇒

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
        prop { (androidId: AndroidId, email: Email, sessionToken: SessionToken, apiKey: ApiKey) ⇒

          WithData(apiKey, email, sessionToken, androidId) { (userId, installationId) ⇒

            val wrongAndroidId = AndroidId(androidId.value.reverse)

            val installation = userPersistenceServices.getInstallationByUserAndAndroidId(
              userId    = userId,
              androidId = wrongAndroidId
            ).transactAndRun

            installation must beLeft[NineCardsError].which { error ⇒
              error must_== InstallationNotFound(s"Installation for android id ${wrongAndroidId.value} not found")
            }
          }
        }
      }
  }

  "getSubscribedInstallationByCollection" should {
    "return an empty list if the table is empty" in {
      prop { (publicIdentifier: PublicIdentifier) ⇒

        val installation = userPersistenceServices.getSubscribedInstallationByCollection(
          publicIdentifier = publicIdentifier.value
        ).transactAndRun

        installation must beRight[List[Installation]](Nil)
      }
    }
    "return a list of installations that are subscribed to the collection" in {
      prop { (
        email: Email,
        sessionToken: SessionToken,
        apiKey: ApiKey,
        collectionData: SharedCollectionData,
        androidId: AndroidId,
        deviceToken: DeviceToken
      ) ⇒

        WithData(apiKey, email, sessionToken, androidId, deviceToken, collectionData) {

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
      prop { (
        email: Email,
        sessionToken: SessionToken,
        apiKey: ApiKey,
        collectionData: SharedCollectionData,
        androidId: AndroidId,
        deviceToken: DeviceToken
      ) ⇒

        WithData(apiKey, email, sessionToken, androidId, deviceToken, collectionData) {

          val installation = userPersistenceServices.getSubscribedInstallationByCollection(
            publicIdentifier = collectionData.publicIdentifier.reverse
          ).transactAndRun

          installation must beRight[List[Installation]](Nil)
        }
      }
    }
  }

}
