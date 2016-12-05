package cards.nine.api.rankings

import cards.nine.api.NineCardsHeaders._
import cards.nine.api.rankings.{ messages ⇒ Api }
import cards.nine.domain.account._
import cards.nine.domain.analytics.RankedWidgetsByMoment
import cards.nine.domain.application.{ Category, Package }
import cards.nine.processes.rankings.messages.{ Get, Reload }
import cards.nine.services.free.domain.Ranking.GoogleAnalyticsRanking
import org.joda.time.{ DateTime, DateTimeZone }
import spray.http.HttpHeaders.RawHeader

private[rankings] object TestData {

  val androidId = AndroidId("f07a13984f6d116a")

  val apiToken = ApiKey("a7db875d-f11e-4b0c-8d7a-db210fd93e1b")

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

    val googleAnalyticsHeaders = List(
      RawHeader(headerGoogleAnalyticsToken, googleAnalyticsToken)
    )
  }

  object Messages {

    val getRankedWidgetsResponse = List.empty[RankedWidgetsByMoment]

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
