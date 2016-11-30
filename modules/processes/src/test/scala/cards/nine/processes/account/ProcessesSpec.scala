package cards.nine.processes.account

import cards.nine.commons.NineCardsErrors._
import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.commons.config.DummyConfig
import cards.nine.domain.account._
import cards.nine.processes.account.messages._
import cards.nine.processes.NineCardsServices._
import cards.nine.processes.TestInterpreters
import cards.nine.processes.utils.HashUtils
import cards.nine.services.free.algebra.{ GoogleApi, User }
import org.mockito.Matchers.{ eq ⇒ mockEq }
import org.specs2.ScalaCheck
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait AccountProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with AccountTestData
  with DummyConfig
  with TestInterpreters {

  trait BasicScope extends Scope {

    implicit val userServices = mock[User.Services[NineCardsServices]]

    implicit val googleApiServices = mock[GoogleApi.Services[NineCardsServices]]

    val accountProcesses = AccountProcesses.processes[NineCardsServices]
  }

  trait UserAndInstallationSuccessfulScope extends BasicScope {

    userServices.getByEmail(Email(mockEq(email.value))) returns NineCardsService.right(user)

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

    userServices.getByEmail(Email(mockEq(email.value))) returns NineCardsService.right(user)

    userServices.getInstallationByUserAndAndroidId(mockEq(userId), AndroidId(mockEq(androidId))) returns
      NineCardsService.left(installationNotFoundError)

    userServices.addInstallation(mockEq(userId), mockEq(None), AndroidId(mockEq(androidId))) returns
      NineCardsService.right(installation)

    userServices.getBySessionToken(SessionToken(mockEq(sessionToken))) returns
      NineCardsService.right(user)
  }

  trait UserAndInstallationFailingScope extends BasicScope {

    userServices.getByEmail(Email(mockEq(email.value))) returns
      NineCardsService.left(userNotFoundError)

    userServices.add(Email(mockEq(email.value)), ApiKey(any[String]), SessionToken(any[String])) returns
      NineCardsService.right(user)

    userServices.addInstallation(mockEq(userId), mockEq(None), AndroidId(mockEq(androidId))) returns
      NineCardsService.right(installation)

    userServices.getBySessionToken(SessionToken(mockEq(sessionToken))) returns
      NineCardsService.left(userNotFoundError)
  }

}

class AccountProcessesSpec
  extends AccountProcessesSpecification
  with ScalaCheck {

  "signUpUser" should {
    "return LoginResponse object when the user exists and installation" in
      new UserAndInstallationSuccessfulScope {
        val signUpUser = accountProcesses.signUpUser(loginRequest)

        signUpUser.foldMap(testInterpreters) must beRight[LoginResponse](loginResponse)
      }

    "return LoginResponse object when the user exists but not installation" in
      new UserSuccessfulAndInstallationFailingScope {
        val signUpUser = accountProcesses.signUpUser(loginRequest)

        signUpUser.foldMap(testInterpreters) must beRight[LoginResponse](loginResponse)
      }

    "return LoginResponse object when there isn't user or installation" in
      new UserAndInstallationFailingScope {
        val signUpUser = accountProcesses.signUpUser(loginRequest)

        signUpUser.foldMap(testInterpreters) must beRight[LoginResponse](loginResponse)
      }
  }

  "updateInstallation" should {
    "return UpdateInstallationResponse object" in new UserAndInstallationSuccessfulScope {
      val signUpInstallation = accountProcesses.updateInstallation(updateInstallationRequest)

      signUpInstallation.foldMap(testInterpreters) must beRight[UpdateInstallationResponse](updateInstallationResponse)
    }
  }

  "checkAuthToken" should {
    "return the userId if there is a user with the given sessionToken and androidId and the " +
      "auth token is valid" in new UserAndInstallationSuccessfulScope {
        val checkAuthToken = accountProcesses.checkAuthToken(
          sessionToken = SessionToken(sessionToken),
          androidId    = AndroidId(androidId),
          authToken    = validAuthToken,
          requestUri   = dummyUrl
        )

        checkAuthToken.foldMap(testInterpreters) must beRight[Long](checkAuthTokenResponse)
      }

    "return the userId for a valid sessionToken and androidId without considering the authToken " +
      "if the debug Mode is enabled" in new UserAndInstallationSuccessfulScope {

        val debugAccountProcesses = AccountProcesses.processes[NineCardsServices](
          googleAPIServices = googleApiServices,
          userServices      = userServices,
          config            = debugConfig,
          hashUtils         = HashUtils.hashUtils
        )

        val checkAuthToken = debugAccountProcesses.checkAuthToken(
          sessionToken = SessionToken(sessionToken),
          androidId    = AndroidId(androidId),
          authToken    = "",
          requestUri   = dummyUrl
        )

        checkAuthToken.foldMap(testInterpreters) must beRight[Long](checkAuthTokenResponse)
      }

    "return None when a wrong auth token is given" in new UserAndInstallationSuccessfulScope {
      val checkAuthToken = accountProcesses.checkAuthToken(
        sessionToken = SessionToken(sessionToken),
        androidId    = AndroidId(androidId),
        authToken    = wrongAuthToken,
        requestUri   = dummyUrl
      )

      checkAuthToken.foldMap(testInterpreters) must beLeft[NineCardsError](authTokenNotValidError)
    }

    "return None if there is no user with the given sessionToken" in
      new UserAndInstallationFailingScope {
        val checkAuthToken = accountProcesses.checkAuthToken(
          sessionToken = SessionToken(sessionToken),
          androidId    = AndroidId(androidId),
          authToken    = validAuthToken,
          requestUri   = dummyUrl
        )

        checkAuthToken.foldMap(testInterpreters) must beLeft[NineCardsError](userNotFoundError)
      }

    "return None if there is no installation with the given androidId that belongs to the user" in
      new UserSuccessfulAndInstallationFailingScope {
        val checkAuthToken = accountProcesses.checkAuthToken(
          sessionToken = SessionToken(sessionToken),
          androidId    = AndroidId(androidId),
          authToken    = validAuthToken,
          requestUri   = dummyUrl
        )

        checkAuthToken.foldMap(testInterpreters) must beLeft[NineCardsError](installationNotFoundError)
      }
  }

  "checkGoogleTokenId" should {
    "return true if the given tokenId is valid" in new BasicScope {

      googleApiServices.getTokenInfo(GoogleIdToken(any[String])) returns NineCardsService.right(tokenInfo)

      accountProcesses
        .checkGoogleTokenId(email, tokenId)
        .foldMap(testInterpreters) must beRight[Unit]
    }

    "return false if the given tokenId is valid but the given email address is different" in new BasicScope {

      googleApiServices.getTokenInfo(GoogleIdToken(any[String])) returns NineCardsService.right(tokenInfo)

      accountProcesses
        .checkGoogleTokenId(wrongEmail, tokenId)
        .foldMap(testInterpreters) must beLeft(wrongEmailAccountError)
    }

    "return a WrongGoogleAuthToken error if the given tokenId is not valid" in new BasicScope {

      googleApiServices.getTokenInfo(GoogleIdToken(any[String])) returns NineCardsService.left(wrongAuthTokenError)

      accountProcesses
        .checkGoogleTokenId(email, tokenId)
        .foldMap(testInterpreters) must beLeft(wrongAuthTokenError)
    }
  }
}

