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

import cards.nine.domain.analytics.DateRange
import cards.nine.services.free.domain.Ranking.RankingError
import cards.nine.services.common.EitherDecoder
import enumeratum.{ Circe ⇒ CirceEnum }
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.{ Decoder, Encoder, Json }

object Encoders {
  import model._
  import order._

  implicit object dateRange extends Encoder[DateRange] {
    import org.joda.time.format.DateTimeFormat
    private[this] val dayFormatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC

    def apply(dateRange: DateRange): Json = Json.obj(
      "startDate" → Encoder.encodeString(dayFormatter.print(dateRange.startDate)),
      "endDate" → Encoder.encodeString(dayFormatter.print(dateRange.endDate))
    )
  }

  implicit private[this] val orderType: Encoder[OrderType] = CirceEnum.encoder(OrderType)

  implicit private[this] val sortOrder: Encoder[SortOrder] = CirceEnum.encoder(SortOrder)

  implicit private[this] val metricType: Encoder[MetricType] = CirceEnum.encoder(MetricType)

  implicit private[this] val dimensionFilterOperator: Encoder[DimensionFilter.Operator] =
    CirceEnum.encoder(DimensionFilter.Operator)

  implicit private[this] val filterLogicalOperator: Encoder[DimensionFilter.LogicalOperator] =
    CirceEnum.encoder(DimensionFilter.LogicalOperator)

  implicit val requestBody: Encoder[RequestBody] = deriveEncoder[RequestBody]

}

object Decoders {
  import model._

  implicit private[this] val metricType: Decoder[MetricType] = CirceEnum.decoder(MetricType)

  implicit val responseBody: Decoder[ResponseBody] = deriveDecoder[ResponseBody]

  implicit val responseBodyError: Decoder[RankingError Either ResponseBody] =
    EitherDecoder.eitherDecoder[RankingError, ResponseBody]

}