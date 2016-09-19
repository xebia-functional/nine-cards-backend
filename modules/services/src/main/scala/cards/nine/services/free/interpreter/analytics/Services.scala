package cards.nine.services.free.interpreter.analytics

import cats.data.Xor
import cards.nine.services.free.domain.rankings._
import org.http4s.Http4s._
import org.http4s.{ EntityDecoder, EntityEncoder, Header, Headers, Method, Request, Query, Uri }
import org.http4s.Uri.{ Authority, RegName }
import org.http4s.circe.{ jsonOf, jsonEncoderOf }
import scalaz.concurrent.Task

class Services(config: Configuration) {

  import model.{ RequestBody, ResponseBody }
  import Encoders._
  import Decoders._

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
      val header: Header = Header("Authorization", s"Bearer ${params.auth.access_token}")
      val body: RequestBody = Converters.buildRequest(scope, config.viewId, params.dateRange)
      Request(method = Method.POST, uri = uri, headers = Headers(header))
        .withBody[RequestBody](body)
    }

    client
      .expect[RankingError Xor ResponseBody](httpRequest)
      .map(_.map(Converters.parseRanking(_, params.rankingLength, scope)))
  }

}

object Services {
  def services(implicit config: Configuration) = new Services(config)
}