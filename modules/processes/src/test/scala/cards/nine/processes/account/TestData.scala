package cards.nine.processes.account

import cards.nine.commons.NineCardsErrors._
import cards.nine.domain.account._
import cards.nine.services.free.domain.{ Installation, TokenInfo, User }
import com.roundeights.hasher.Hasher

private[account] trait AccountTestData {

  import messages._

  val email = Email("valid.email@test.com")

  val wrongEmail = Email("wrong.email@test.com")

  val tokenId = GoogleIdToken("eyJhbGciOiJSUzI1NiIsImtpZCI6IjcxMjI3MjFlZWQwYjQ1YmUxNWUzMGI2YThhOThjOTM3ZTJlNmQxN")

  val tokenInfo = TokenInfo("true", email.value)

  val wrongEmailAccountError = WrongEmailAccount(message = "The given email account is not valid")

  val wrongAuthTokenError = WrongGoogleAuthToken(message = "Invalid Value")

  val userId = 1l

  val apiKey = "60b32e59-0d87-4705-a454-2e5b38bec13b"

  val wrongApiKey = "f93cff07-32c9-4995-8e80-a8adfafbf296"

  val sessionToken = "1d1afeea-c7ec-45d8-a6f8-825b836f2785"

  val banned = false

  val user = User(userId, email, SessionToken(sessionToken), ApiKey(apiKey), banned)

  val userNotFoundError = UserNotFound("The user doesn't exist")

  val androidId = "f07a13984f6d116a"

  val googleTokenId = "hd-w2tmEe7SZ_8vXhw_3f1iNnsrAqkpEvbPkFIo9oZeAq26u"

  val deviceToken = "abc"

  val installationId = 1l

  val loginRequest = LoginRequest(email, AndroidId(androidId), SessionToken(sessionToken), GoogleIdToken(googleTokenId))

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

