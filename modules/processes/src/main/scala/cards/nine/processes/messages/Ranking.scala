package cards.nine.processes.messages

import cards.nine.domain.analytics.{ CountryIsoCode, GeoScope, RankingParams, UpdateRankingSummary }
import cards.nine.domain.application.Package
import cards.nine.services.free.domain.Ranking._

object rankings {

  object Reload {

    case class Request(scope: GeoScope, params: RankingParams)

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