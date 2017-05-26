/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.services.free.interpreter.analytics

import cards.nine.commons.NineCardsErrors._
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.catscalaz.TaskInstances._
import cards.nine.commons.config.Domain.GoogleAnalyticsConfiguration
import cards.nine.domain.analytics.{ CountryIsoCode, RankingParams }
import cards.nine.services.free.algebra.GoogleAnalytics._
import cards.nine.services.free.domain.Ranking._
import cards.nine.services.free.interpreter.analytics.HttpMessagesFactory._
import cats.instances.all._
import cats.syntax.either._
import cats.syntax.traverse._
import org.http4s.Http4s._
import org.http4s.Uri.{ Authority, RegName }
import org.http4s._
import org.http4s.circe.{ jsonEncoderOf, jsonOf }

import scalaz.concurrent.Task

class Services(config: GoogleAnalyticsConfiguration) extends Handler[Task] {

  import Encoders._
  import model.{ RequestBody, ResponseBody }

  private[this] val client = org.http4s.client.blaze.PooledHttp1Client()

  private[this] val uri: Uri = Uri(
    scheme    = Option(config.protocol.ci),
    authority = Option(Authority(host = RegName(config.host), port = config.port)),
    path      = config.path
  )

  implicit private[this] val requestBodyEntity: EntityEncoder[RequestBody] =
    jsonEncoderOf[RequestBody]

  implicit private[this] val responseEntity: EntityDecoder[RankingError Either ResponseBody] =
    jsonOf[RankingError Either ResponseBody](Decoders.responseBodyError)

  override def getCountriesWithRanking(params: RankingParams): Task[Result[CountriesWithRanking]] = {
    val request = CountriesWithRankingReport.buildRequest(params.dateRange, config.viewId)
    doRequest(params, CountriesWithRankingReport.parseResponse)(request).map(_.joinRight)
  }

  override def getRanking(
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

}

object Services {
  def services(config: GoogleAnalyticsConfiguration) = new Services(config)
}