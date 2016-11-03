package cards.nine.processes

import cards.nine.commons.config.DummyConfig
import cards.nine.commons.NineCardsErrors.{ AuthTokenNotValid, InstallationNotFound, NineCardsError, UserNotFound }
import cards.nine.commons.NineCardsService
import cards.nine.domain.account._
import cards.nine.processes.NineCardsServices._
import cards.nine.processes.messages.InstallationsMessages._
import cards.nine.processes.messages.UserMessages.{ LoginRequest, LoginResponse }
import cards.nine.processes.utils.HashUtils
import cards.nine.services.free.algebra
import cards.nine.services.free.domain.{ Installation, User }
import com.roundeights.hasher.Hasher
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.ScalaCheck
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait UserProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with UserProcessesContext
  with DummyConfig
  with TestInterpreters {

  trait BasicScope extends Scope {
    implicit val userServices = mock[algebra.User.Services[NineCardsServices]]

    val userProcesses = UserProcesses.processes[NineCardsServices]
  }

  trait UserAndInstallationSuccessfulScope extends BasicScope {

    userServices.getByEmail(Email(mockEq(email))) returns NineCardsService.right(user)

    userServices.getInstallationByUserAndAndroidId(mockEq(userId), AndroidId(mockEq(androidId))) returns
      NineCardsService.right(installation)

    userServices.updateInstallation(mockEq(userId), mockEq(Option(DeviceToken(deviceToken))), AndroidId(mockEq(androidId))) returns
      NineCardsService.right(installation)

    userServices.getBySessionToken(SessionToken(mockEq(sessionToken))) returns
      NineCardsService.right(user)

    userServices.getInstallationByUserAndAndroidId(mockEq(userId), AndroidId(mockEq(androidId))) returns
      NineCardsService.right(installation)
  }

  trait UserSuccessfulAndInstallationFailingScope extends BasicScope {

    userServices.getByEmail(Email(mockEq(email))) returns NineCardsService.right(user)

    userServices.getInstallationByUserAndAndroidId(mockEq(userId), AndroidId(mockEq(androidId))) returns
      NineCardsService.left(installationNotFoundError)

    userServices.addInstallation(mockEq(userId), mockEq(None), AndroidId(mockEq(androidId))) returns
      NineCardsService.right(installation)

    userServices.getBySessionToken(SessionToken(mockEq(sessionToken))) returns
      NineCardsService.right(user)
  }

  trait UserAndInstallationFailingScope extends BasicScope {

    userServices.getByEmail(Email(mockEq(email))) returns
      NineCardsService.left(userNotFoundError)

    userServices.add(Email(mockEq(email)), ApiKey(any[String]), SessionToken(any[String])) returns
      NineCardsService.right(user)

    userServices.addInstallation(mockEq(userId), mockEq(None), AndroidId(mockEq(androidId))) returns
      NineCardsService.right(installation)

    userServices.getBySessionToken(SessionToken(mockEq(sessionToken))) returns
      NineCardsService.left(userNotFoundError)
  }

}

trait UserProcessesContext {

  val email = "valid.email@test.com"

  val userId = 1l

  val apiKey = "60b32e59-0d87-4705-a454-2e5b38bec13b"

  val wrongApiKey = "f93cff07-32c9-4995-8e80-a8adfafbf296"

  val sessionToken = "1d1afeea-c7ec-45d8-a6f8-825b836f2785"

  val banned = false

  val user = User(userId, Email(email), SessionToken(sessionToken), ApiKey(apiKey), banned)

  val userNotFoundError = UserNotFound("The user doesn't exist")

  val androidId = "f07a13984f6d116a"

  val googleTokenId = "hd-w2tmEe7SZ_8vXhw_3f1iNnsrAqkpEvbPkFIo9oZeAq26u"

  val deviceToken = "abc"

  val installationId = 1l

  val loginRequest = LoginRequest(Email(email), AndroidId(androidId), SessionToken(sessionToken), GoogleIdToken(googleTokenId))

  val loginResponse = LoginResponse(ApiKey(apiKey), SessionToken(sessionToken))

  val updateInstallationRequest = UpdateInstallationRequest(userId, AndroidId(androidId), Option(DeviceToken(deviceToken)))

  val updateInstallationResponse = UpdateInstallationResponse(AndroidId(androidId), Option(DeviceToken(deviceToken)))

  val installation = Installation(installationId, userId, Option(DeviceToken(deviceToken)), AndroidId(androidId))

  val installationNotFoundError = InstallationNotFound("The installation doesn't exist")

  val checkAuthTokenResponse = userId

  val dummyUrl = "http://localhost/dummy"

  val validAuthToken = Hasher(dummyUrl).hmac(apiKey).sha512.hex

  val wrongAuthToken = Hasher(dummyUrl).hmac(wrongApiKey).sha512.hex

  val authTokenNotValidError = AuthTokenNotValid("The provided auth token is not valid")
}

class UserProcessesSpec
  extends UserProcessesSpecification
  with ScalaCheck {

  "signUpUser" should {
    "return LoginResponse object when the user exists and installation" in
      new UserAndInstallationSuccessfulScope {
        val signUpUser = userProcesses.signUpUser(loginRequest)

        signUpUser.foldMap(testInterpreters) must beRight[LoginResponse](loginResponse)
      }

    "return LoginResponse object when the user exists but not installation" in
      new UserSuccessfulAndInstallationFailingScope {
        val signUpUser = userProcesses.signUpUser(loginRequest)

        signUpUser.foldMap(testInterpreters) must beRight[LoginResponse](loginResponse)
      }

    "return LoginResponse object when there isn't user or installation" in
      new UserAndInstallationFailingScope {
        val signUpUser = userProcesses.signUpUser(loginRequest)

        signUpUser.foldMap(testInterpreters) must beRight[LoginResponse](loginResponse)
      }
  }

  "updateInstallation" should {
    "return UpdateInstallationResponse object" in new UserAndInstallationSuccessfulScope {
      val signUpInstallation = userProcesses.updateInstallation(updateInstallationRequest)

      signUpInstallation.foldMap(testInterpreters) must beRight[UpdateInstallationResponse](updateInstallationResponse)
    }
  }

  "checkAuthToken" should {
    "return the userId if there is a user with the given sessionToken and androidId and the " +
      "auth token is valid" in new UserAndInstallationSuccessfulScope {
        val checkAuthToken = userProcesses.checkAuthToken(
          sessionToken = SessionToken(sessionToken),
          androidId    = AndroidId(androidId),
          authToken    = validAuthToken,
          requestUri   = dummyUrl
        )

        checkAuthToken.foldMap(testInterpreters) must beRight[Long](checkAuthTokenResponse)
      }

    "return the userId for a valid sessionToken and androidId without considering the authToken " +
      "if the debug Mode is enabled" in new UserAndInstallationSuccessfulScope {

        val debugUserProcesses = UserProcesses.processes[NineCardsServices](
          userServices = userServices,
          config       = debugConfig,
          hashUtils    = HashUtils.hashUtils
        )

        val checkAuthToken = debugUserProcesses.checkAuthToken(
          sessionToken = SessionToken(sessionToken),
          androidId    = AndroidId(androidId),
          authToken    = "",
          requestUri   = dummyUrl
        )

        checkAuthToken.foldMap(testInterpreters) must beRight[Long](checkAuthTokenResponse)
      }

    "return None when a wrong auth token is given" in new UserAndInstallationSuccessfulScope {
      val checkAuthToken = userProcesses.checkAuthToken(
        sessionToken = SessionToken(sessionToken),
        androidId    = AndroidId(androidId),
        authToken    = wrongAuthToken,
        requestUri   = dummyUrl
      )

      checkAuthToken.foldMap(testInterpreters) must beLeft[NineCardsError](authTokenNotValidError)
    }

    "return None if there is no user with the given sessionToken" in
      new UserAndInstallationFailingScope {
        val checkAuthToken = userProcesses.checkAuthToken(
          sessionToken = SessionToken(sessionToken),
          androidId    = AndroidId(androidId),
          authToken    = validAuthToken,
          requestUri   = dummyUrl
        )

        checkAuthToken.foldMap(testInterpreters) must beLeft[NineCardsError](userNotFoundError)
      }

    "return None if there is no installation with the given androidId that belongs to the user" in
      new UserSuccessfulAndInstallationFailingScope {
        val checkAuthToken = userProcesses.checkAuthToken(
          sessionToken = SessionToken(sessionToken),
          androidId    = AndroidId(androidId),
          authToken    = validAuthToken,
          requestUri   = dummyUrl
        )

        checkAuthToken.foldMap(testInterpreters) must beLeft[NineCardsError](installationNotFoundError)
      }
  }
}
