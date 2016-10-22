package cards.nine.services.free.interpreter.analytics

import cards.nine.commons.NineCardsErrors._
import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.analytics.{ CountryName, RankingParams }
import cards.nine.services.free.algebra.GoogleAnalytics._
import cards.nine.services.free.domain.Ranking._
import cats.syntax.either._
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

  implicit private[this] val responseEntity: EntityDecoder[RankingError Either ResponseBody] =
    jsonOf[RankingError Either ResponseBody](Decoders.responseBodyError)

  def getRanking(name: Option[CountryName], params: RankingParams): Task[Result[GoogleAnalyticsRanking]] = {
    val httpRequest: Task[Request] = {
      val header: Header = Header("Authorization", s"Bearer ${params.auth.value}")
      val body: RequestBody = Converters.buildRequest(name, config.viewId, params.dateRange)
      Request(method = Method.POST, uri = uri, headers = Headers(header))
        .withBody[RequestBody](body)
    }

    client
      .expect[RankingError Either ResponseBody](httpRequest)
      .map {
        case Left(error) ⇒ Either.left(handleGoogleAnalyticsError(error))
        case Right(response) ⇒ Converters.parseRanking(response, params.rankingLength, name)
      }
  }

  def handleGoogleAnalyticsError(error: RankingError) =
    error.code match {
      case Status.BadRequest.code ⇒ HttpBadRequest("")
      case Status.NotFound.code ⇒ HttpNotFound("")
      case Status.Unauthorized.code ⇒ HttpUnauthorized("")
      case _ ⇒ GoogleAnalyticsServerError("")
    }

  def apply[A](fa: Ops[A]): Task[A] = fa match {
    case GetRanking(name, params) ⇒ getRanking(name, params)
  }
}

object Services {
  def services(implicit config: Configuration) = new Services(config)
}