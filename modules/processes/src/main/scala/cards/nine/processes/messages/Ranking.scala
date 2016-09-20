package cards.nine.processes.messages

import cats.data.Xor
import cards.nine.services.free.domain.rankings._

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

    case class DeviceApp(packageName: String)

    case class RankedDeviceApp(packageName: String, ranking: Option[Int])
  }

}