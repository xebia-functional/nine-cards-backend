package cards.nine.services.free.interpreter.analytics

import cards.nine.commons.NineCardsErrors._
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.TaskInstances._
import cards.nine.domain.analytics.{ CountryIsoCode, RankingParams }
import cards.nine.services.free.algebra.GoogleAnalytics._
import cards.nine.services.free.domain.Ranking._
import cards.nine.services.free.interpreter.analytics.HttpMessagesFactory._
import cats.instances.all._
import cats.syntax.either._
import cats.syntax.traverse._
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

  def getCountriesWithRanking(params: RankingParams): Task[Result[CountriesWithRanking]] = {
    val request = CountriesWithRankingReport.buildRequest(params.dateRange, config.viewId)
    doRequest(params, CountriesWithRankingReport.parseResponse)(request).map(_.joinRight)
  }

  def getRanking(
    code: Option[CountryIsoCode],
    categories: List[String],
    params: RankingParams
  ): Task[Result[GoogleAnalyticsRanking]] = {

    RankingsByCountryReport
      .buildRequestsForCountry(code, categories, params.dateRange, params.rankingLength, config.viewId)
      .toList
      .traverse(doRequest(params, RankingsByCountryReport.parseResponse))
      .map(responses ⇒ responses.sequenceU.map(r ⇒ GoogleAnalyticsRanking(r.flatten.toMap)))
  }

  private[this] def doRequest[A](params: RankingParams, parser: ResponseBody ⇒ A)(requestBody: RequestBody) = {
    def handleGoogleAnalyticsError(error: RankingError) =
      error.code match {
        case Status.BadRequest.code ⇒ HttpBadRequest(error.message)
        case Status.NotFound.code ⇒ HttpNotFound(error.message)
        case Status.Unauthorized.code ⇒ HttpUnauthorized(error.message)
        case _ ⇒ GoogleAnalyticsServerError(error.message)
      }

    val httpRequest: Task[Request] =
      Request(method = Method.POST, uri = uri)
        .withBody[RequestBody](requestBody)
        .putHeaders(Header("Authorization", s"Bearer ${params.auth.value}"))

    client
      .expect[RankingError Either ResponseBody](httpRequest)
      .map { response ⇒
        response.bimap(
          handleGoogleAnalyticsError,
          parser
        )
      }
  }

  def apply[A](fa: Ops[A]): Task[A] = fa match {
    case GetCountriesWithRanking(params) ⇒ getCountriesWithRanking(params)
    case GetRanking(code, categories, params) ⇒ getRanking(code, categories, params)
  }
}

object Services {
  def services(implicit config: Configuration) = new Services(config)
}