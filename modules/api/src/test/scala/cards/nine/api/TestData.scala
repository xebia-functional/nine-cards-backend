package cards.nine.api

import cards.nine.api.NineCardsHeaders._
import cards.nine.api.messages.GooglePlayMessages._
import cards.nine.api.messages.InstallationsMessages.ApiUpdateInstallationRequest
import cards.nine.api.messages.UserMessages.ApiLoginRequest
import cards.nine.api.messages.{ rankings ⇒ Api }
import cards.nine.domain.account._
import cards.nine.domain.analytics.RankedWidgetsByMoment
import cards.nine.domain.application.{ CardList, Category, FullCard, Package }
import cards.nine.processes.messages.InstallationsMessages._
import cards.nine.processes.messages.UserMessages.{ LoginRequest, LoginResponse }
import cards.nine.processes.messages.rankings.{ Get, Reload }
import cards.nine.services.free.domain.Ranking.GoogleAnalyticsRanking
import org.joda.time.{ DateTime, DateTimeZone }
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

  val icon = "path-to-icon"

  val limit = 20

  val limitPerApp = 25

  val location = Option("US")

  val marketLocalization = "en-us"

  val now = DateTime.now

  val packagesName = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany",
    "earth.europe.france",
    "earth.europe.portugal",
    "earth.europe.spain"
  ) map Package

  val deviceApps = Map("countries" → packagesName)

  val excludePackages = packagesName.filter(_.value.length > 18)

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

  object Messages {

    val apiGetRecommendationsByCategoryRequest = ApiGetRecommendationsByCategoryRequest(
      excludePackages = excludePackages,
      limit           = limit
    )

    val apiGetRecommendationsForAppsRequest = ApiGetRecommendationsForAppsRequest(
      packages        = packagesName,
      excludePackages = excludePackages,
      limit           = limit,
      limitPerApp     = Option(limitPerApp)
    )

    val apiRankByMomentsRequest = ApiRankByMomentsRequest(
      location = location,
      items    = packagesName,
      moments  = moments,
      limit    = limit
    )

    val getRankedWidgetsResponse = List.empty[RankedWidgetsByMoment]

    val getRecommendationsByCategoryResponse = CardList[FullCard](Nil, Nil)

    val apiLoginRequest = ApiLoginRequest(email, androidId, tokenId)

    val apiUpdateInstallationRequest = ApiUpdateInstallationRequest(deviceToken)

    val getAppsInfoResponse = CardList[FullCard](Nil, Nil)

    val loginRequest = LoginRequest(email, androidId, sessionToken, tokenId)

    val loginResponse = LoginResponse(apiToken, sessionToken)

    val updateInstallationRequest = UpdateInstallationRequest(userId, androidId, deviceToken)

    val updateInstallationResponse = UpdateInstallationResponse(androidId, deviceToken)

    object rankings {

      val ranking = GoogleAnalyticsRanking(Map(
        Category.SOCIAL.entryName → List(Package("testApp"))
      ))
      val getResponse = Get.Response(ranking)

      val apiRanking = Api.Ranking(
        Map(
          Category.SOCIAL.entryName → List("socialite", "socialist").map(Package),
          Category.COMMUNICATION.entryName → List("es.elpais", "es.elmundo", "uk.theguardian").map(Package)
        )
      )

      val reloadResponse = Reload.Response()
      val startDate: DateTime = new DateTime(2016, 7, 15, 0, 0, DateTimeZone.UTC)
      val endDate: DateTime = new DateTime(2016, 8, 21, 0, 0, DateTimeZone.UTC)
      val reloadApiRequest = Api.Reload.Request(startDate, endDate, 5)
      val reloadApiResponse = Api.Reload.Response()

    }
  }

  object Paths {

    val apiDocs = "/apiDocs/index.html"

    val installations = "/installations"

    val invalid = "/chalkyTown"

    val login = "/login"

    val rankWidgets = "/widgets/rank"

    val recommendationsByCategory = "/recommendations/SOCIAL"

    val recommendationsForApps = "/recommendations"

  }

}
