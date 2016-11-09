package cards.nine.processes.messages

import cards.nine.domain.analytics._
import cards.nine.domain.application.Package
import cards.nine.domain.oauth.ServiceAccount
import cards.nine.domain.pagination.Page
import cards.nine.services.free.domain.Ranking._

object rankings {

  object Reload {

    case class Request(
      dateRange: DateRange,
      rankingLength: Int,
      serviceAccount: ServiceAccount,
      pageParams: Page
    )

    case class Response()

    case class SummaryResponse(
      countriesWithoutRanking: List[CountryIsoCode],
      countriesWithRanking: List[UpdateRankingSummary]
    )

  }

  object Get {

    case class Request(scope: GeoScope)

    case class Response(ranking: GoogleAnalyticsRanking)

  }

  object GetRankedDeviceApps {

    case class DeviceApp(packageName: Package)

  }

}