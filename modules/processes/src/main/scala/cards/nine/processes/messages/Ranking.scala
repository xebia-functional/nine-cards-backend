package cards.nine.processes.messages

import cards.nine.domain.analytics.GeoScope
import cards.nine.domain.application.Package
import cards.nine.services.free.domain.rankings._
import cats.data.Xor

object rankings {

  object Reload {

    case class Request(scope: GeoScope, params: RankingParams)

    case class Response()

    case class Error(code: Int, message: String, status: String) extends Throwable

    type XorResponse = Xor[Error, Response]
  }

  object Get {

    case class Request(scope: GeoScope)

    case class Response(ranking: Ranking)

  }

  object GetRankedDeviceApps {
    case class DeviceApp(packageName: Package)
  }

}