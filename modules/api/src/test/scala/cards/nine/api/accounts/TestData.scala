package cards.nine.api.accounts

import cards.nine.api.NineCardsHeaders._
import cards.nine.api.accounts.messages.{ ApiLoginRequest, ApiUpdateInstallationRequest }
import cards.nine.domain.account._
import cards.nine.processes.account.messages._
import org.joda.time.DateTime
import spray.http.HttpHeaders.RawHeader

private[accounts] object TestData {

  val androidId = AndroidId("f07a13984f6d116a")

  val apiToken = ApiKey("a7db875d-f11e-4b0c-8d7a-db210fd93e1b")

  val authToken = "c8abd539-d912-4eff-8d3c-679307defc71"

  val deviceToken = Option(DeviceToken("d897b6f1-c6a9-42bd-bf42-c787883c7d3e"))

  val email = Email("valid.email@test.com")

  val failingAuthToken = "a439c00e"

  val location = Option("US")

  val now = DateTime.now

  val sessionToken = SessionToken("1d1afeea-c7ec-45d8-a6f8-825b836f2785")

  val tokenId = GoogleIdToken("6c7b303e-585e-4fe8-8b6f-586547317331-7f9b12dd-8946-4285-a72a-746e482834dd")

  val userId = 1l

  object Headers {

    val userInfoHeaders = List(
      RawHeader(headerAndroidId, androidId.value),
      RawHeader(headerSessionToken, sessionToken.value),
      RawHeader(headerAuthToken, authToken)
    )

    val failingUserInfoHeaders = List(
      RawHeader(headerAndroidId, androidId.value),
      RawHeader(headerSessionToken, sessionToken.value),
      RawHeader(headerAuthToken, failingAuthToken)
    )

  }

  object Messages {

    val apiLoginRequest = ApiLoginRequest(email, androidId, tokenId)

    val apiUpdateInstallationRequest = ApiUpdateInstallationRequest(deviceToken)

    val loginRequest = LoginRequest(email, androidId, sessionToken, tokenId)

    val loginResponse = LoginResponse(apiToken, sessionToken)

    val updateInstallationRequest = UpdateInstallationRequest(userId, androidId, deviceToken)

    val updateInstallationResponse = UpdateInstallationResponse(androidId, deviceToken)

  }

  object Paths {

    val installations = "/installations"

    val login = "/login"

  }

}
