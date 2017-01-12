package cards.nine.api.rankings

import cards.nine.api.NineCardsHeaders._
import cards.nine.api.rankings.{ messages ⇒ Api }
import cards.nine.domain.analytics.RankedWidgetsByMoment
import cards.nine.domain.application.{ Category, Package }
import cards.nine.processes.rankings.messages.{ Get, Reload }
import cards.nine.services.free.domain.Ranking.GoogleAnalyticsRanking
import org.joda.time.{ DateTime, DateTimeZone }
import spray.http.HttpHeaders.RawHeader

private[rankings] object TestData {

  val googleAnalyticsToken = "yada-yada-yada"

  val location = Option("US")

  val now = DateTime.now

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
