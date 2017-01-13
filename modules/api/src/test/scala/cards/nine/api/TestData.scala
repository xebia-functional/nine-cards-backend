package cards.nine.api

import cards.nine.api.NineCardsHeaders._
import cards.nine.domain.account._
import org.joda.time.DateTime
import spray.http.HttpHeaders.RawHeader

object TestData {

  val androidId = AndroidId("f07a13984f6d116a")

  val apiToken = ApiKey("a7db875d-f11e-4b0c-8d7a-db210fd93e1b")

  val author = "John Doe"

  val authToken = "c8abd539-d912-4eff-8d3c-679307defc71"

  val category = "SOCIAL"

  val deviceToken = Option(DeviceToken("d897b6f1-c6a9-42bd-bf42-c787883c7d3e"))

  val email = Email("valid.email@test.com")

  val failingAuthToken = "a439c00e"

  val googlePlayToken = "8d8f9814"

  val googleAnalyticsToken = "yada-yada-yada"

  val location = Option("US")

  val marketLocalization = "en-us"

  val now = DateTime.now

  val moments = List("HOME", "NIGHT")

  val sessionToken = SessionToken("1d1afeea-c7ec-45d8-a6f8-825b836f2785")

  val tokenId = GoogleIdToken("6c7b303e-585e-4fe8-8b6f-586547317331-7f9b12dd-8946-4285-a72a-746e482834dd")

  val userId = 1l

  object Headers {

    val userInfoHeaders = List(
      RawHeader(headerAndroidId, androidId.value),
      RawHeader(headerSessionToken, sessionToken.value),
      RawHeader(headerAuthToken, authToken)
    )

    val googlePlayHeaders = List(
      RawHeader(headerGooglePlayToken, googlePlayToken),
      RawHeader(headerMarketLocalization, marketLocalization)
    )

    val failingUserInfoHeaders = List(
      RawHeader(headerAndroidId, androidId.value),
      RawHeader(headerSessionToken, sessionToken.value),
      RawHeader(headerAuthToken, failingAuthToken)
    )

    val googleAnalyticsHeaders = List(
      RawHeader(headerGoogleAnalyticsToken, googleAnalyticsToken)
    )
  }

  object Paths {

    val apiDocs = "/apiDocs/index.html"

    val invalid = "/chalkyTown"

  }

}
