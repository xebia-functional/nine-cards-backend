package cards.nine.services.free.interpreter.analytics

import cards.nine.domain.analytics.GeoScope
import cards.nine.services.free.algebra.GoogleAnalytics._
import cards.nine.services.free.domain.Ranking._
import cats.data.Xor
import cats.~>
import org.http4s.Http4s._
import org.http4s.Uri.{ Authority, RegName }
import org.http4s._
import org.http4s.circe.{ jsonEncoderOf, jsonOf }

import scalaz.concurrent.Task

class Services(config: Configuration) extends (Ops ~> Task) {

  import Encoders._
  import model.{ RequestBody, ResponseBody }

  private[this] val client = org.http4s.client.blaze.PooledHttp1Client()

  private[this] val uri: Uri = Uri(
    scheme    = Option(config.protocol.ci),
    authority = Option(Authority(host = RegName(config.host), port = config.port)),
    path      = config.uri
  )

  implicit private[this] val requestBodyEntity: EntityEncoder[RequestBody] =
    jsonEncoderOf[RequestBody]

  implicit private[this] val responseEntity: EntityDecoder[RankingError Xor ResponseBody] =
    jsonOf[RankingError Xor ResponseBody](Decoders.responseBodyError)

  def getRanking(scope: GeoScope, params: RankingParams): Task[TryRanking] = {
    val httpRequest: Task[Request] = {
      val header: Header = Header("Authorization", s"Bearer ${params.auth.value}")
      val body: RequestBody = Converters.buildRequest(scope, config.viewId, params.dateRange)
      Request(method = Method.POST, uri = uri, headers = Headers(header))
        .withBody[RequestBody](body)
    }

    client
      .expect[RankingError Xor ResponseBody](httpRequest)
      .map(_.map(Converters.parseRanking(_, params.rankingLength, scope)))
  }

  def apply[A](fa: Ops[A]): Task[A] = fa match {
    case GetRanking(geoScope, params) ⇒ getRanking(geoScope, params)
  }
}

object Services {
  def services(implicit config: Configuration) = new Services(config)
}