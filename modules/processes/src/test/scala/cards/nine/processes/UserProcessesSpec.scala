package cards.nine.processes

import cards.nine.processes.NineCardsServices._
import cards.nine.processes.messages.InstallationsMessages._
import cards.nine.processes.messages.UserMessages.{ LoginRequest, LoginResponse }
import cards.nine.processes.utils.{ DummyNineCardsConfig, HashUtils }
import cards.nine.services.free.algebra.DBResult.DBOps
import cards.nine.services.free.domain.{ Installation, User }
import cards.nine.services.persistence.{ UserPersistenceServices, _ }
import com.roundeights.hasher.Hasher
import doobie.imports._
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.ScalaCheck
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.Scalaz._

trait UserProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with UserProcessesContext
  with DummyNineCardsConfig
  with TestInterpreters {

  trait BasicScope extends Scope {
    implicit val userPersistenceServices: UserPersistenceServices = mock[UserPersistenceServices]
    val userProcesses = new UserProcesses[NineCardsServices]
  }

  trait UserAndInstallationSuccessfulScope extends BasicScope {

    userPersistenceServices.getUserByEmail(mockEq(email)) returns Option(user).point[ConnectionIO]

    userPersistenceServices.getInstallationByUserAndAndroidId(mockEq(userId), mockEq(androidId)) returns Option(installation).point[ConnectionIO]

    userPersistenceServices.updateInstallation[Installation](mockEq(userId), mockEq(Option(deviceToken)), mockEq(androidId))(any) returns installation.point[ConnectionIO]

    userPersistenceServices.getUserBySessionToken(mockEq(sessionToken)) returns Option(user).point[ConnectionIO]

    userPersistenceServices.getInstallationByUserAndAndroidId(mockEq(userId), mockEq(androidId)) returns Option(installation).point[ConnectionIO]
  }

  trait UserSuccessfulAndInstallationFailingScope extends BasicScope {

    userPersistenceServices.getUserByEmail(mockEq(email)) returns Option(user).point[ConnectionIO]

    userPersistenceServices.getInstallationByUserAndAndroidId(mockEq(userId), mockEq(androidId)) returns nonExistingInstallation.point[ConnectionIO]

    userPersistenceServices.createInstallation[Installation](mockEq(userId), mockEq(None), mockEq(androidId))(any) returns installation.point[ConnectionIO]

    userPersistenceServices.getUserBySessionToken(mockEq(sessionToken)) returns Option(user).point[ConnectionIO]
  }

  trait UserAndInstallationFailingScope extends BasicScope {

    userPersistenceServices.getUserByEmail(email) returns nonExistingUser.point[ConnectionIO]

    userPersistenceServices.addUser[User](mockEq(email), any[String], any[String])(any) returns user.point[ConnectionIO]

    userPersistenceServices.createInstallation[Installation](mockEq(userId), mockEq(None), mockEq(androidId))(any) returns installation.point[ConnectionIO]

    userPersistenceServices.getUserBySessionToken(mockEq(sessionToken)) returns nonExistingUser.point[ConnectionIO]
  }

}

trait UserProcessesContext {

  val email = "valid.email@test.com"

  val userId = 1l

  val apiKey = "60b32e59-0d87-4705-a454-2e5b38bec13b"

  val wrongApiKey = "f93cff07-32c9-4995-8e80-a8adfafbf296"

  val sessionToken = "1d1afeea-c7ec-45d8-a6f8-825b836f2785"

  val banned = false

  val user = User(userId, email, sessionToken, apiKey, banned)

  val nonExistingUser: Option[User] = None

  val androidId = "f07a13984f6d116a"

  val googleTokenId = "hd-w2tmEe7SZ_8vXhw_3f1iNnsrAqkpEvbPkFIo9oZeAq26u"

  val deviceToken = "abc"

  val installationId = 1l

  val loginRequest = LoginRequest(email, androidId, sessionToken, googleTokenId)

  val loginResponse = LoginResponse(apiKey, sessionToken)

  val updateInstallationRequest = UpdateInstallationRequest(userId, androidId, Option(deviceToken))

  val updateInstallationResponse = UpdateInstallationResponse(androidId, Option(deviceToken))

  val installation = Installation(installationId, userId, Option(deviceToken), androidId)

  val nonExistingInstallation: Option[Installation] = None

  val checkAuthTokenResponse = Option(userId)

  val dummyUrl = "http://localhost/dummy"

  val validAuthToken = Hasher(dummyUrl).hmac(apiKey).sha512.hex

  val wrongAuthToken = Hasher(dummyUrl).hmac(wrongApiKey).sha512.hex
}

class UserProcessesSpec
  extends UserProcessesSpecification
  with ScalaCheck {

  "signUpUser" should {
    "return LoginResponse object when the user exists and installation" in
      new UserAndInstallationSuccessfulScope {
        val signUpUser = userProcesses.signUpUser(loginRequest)
        signUpUser.foldMap(testInterpreters) shouldEqual loginResponse
      }

    "return LoginResponse object when the user exists but not installation" in
      new UserSuccessfulAndInstallationFailingScope {
        val signUpUser = userProcesses.signUpUser(loginRequest)
        signUpUser.foldMap(testInterpreters) shouldEqual loginResponse
      }

    "return LoginResponse object when there isn't user or installation" in
      new UserAndInstallationFailingScope {
        val signUpUser = userProcesses.signUpUser(loginRequest)
        signUpUser.foldMap(testInterpreters) shouldEqual loginResponse
      }
  }

  "updateInstallation" should {
    "return UpdateInstallationResponse object" in new UserAndInstallationSuccessfulScope {
      val signUpInstallation = userProcesses.updateInstallation(updateInstallationRequest)
      signUpInstallation.foldMap(testInterpreters) shouldEqual updateInstallationResponse
    }
  }

  "checkAuthToken" should {
    "return the userId if there is a user with the given sessionToken and androidId and the " +
      "auth token is valid" in new UserAndInstallationSuccessfulScope {
        val checkAuthToken = userProcesses.checkAuthToken(
          sessionToken = sessionToken,
          androidId    = androidId,
          authToken    = validAuthToken,
          requestUri   = dummyUrl
        )

        checkAuthToken.foldMap(testInterpreters) shouldEqual checkAuthTokenResponse
      }

    "return the userId for a valid sessionToken and androidId without considering the authToken " +
      "if the debug Mode is enabled" in new UserAndInstallationSuccessfulScope {

        val debugUserProcesses = UserProcesses.userProcesses[NineCardsServices](
          userPersistenceServices, dummyConfig(debugMode = true), HashUtils.hashUtils, DBOps.dbOps
        )

        val checkAuthToken = debugUserProcesses.checkAuthToken(
          sessionToken = sessionToken,
          androidId    = androidId,
          authToken    = "",
          requestUri   = dummyUrl
        )

        checkAuthToken.foldMap(testInterpreters) shouldEqual checkAuthTokenResponse
      }

    "return None when a wrong auth token is given" in new UserAndInstallationSuccessfulScope {
      val checkAuthToken = userProcesses.checkAuthToken(
        sessionToken = sessionToken,
        androidId    = androidId,
        authToken    = wrongAuthToken,
        requestUri   = dummyUrl
      )

      checkAuthToken.foldMap(testInterpreters) shouldEqual None
    }

    "return None if there is no user with the given sessionToken" in
      new UserAndInstallationFailingScope {
        val checkAuthToken = userProcesses.checkAuthToken(
          sessionToken = sessionToken,
          androidId    = androidId,
          authToken    = validAuthToken,
          requestUri   = dummyUrl
        )

        checkAuthToken.foldMap(testInterpreters) should beNone
      }

    "return None if there is no installation with the given androidId that belongs to the user" in
      new UserSuccessfulAndInstallationFailingScope {
        val checkAuthToken = userProcesses.checkAuthToken(
          sessionToken = sessionToken,
          androidId    = androidId,
          authToken    = validAuthToken,
          requestUri   = dummyUrl
        )

        checkAuthToken.foldMap(testInterpreters) should beNone
      }
  }
}
