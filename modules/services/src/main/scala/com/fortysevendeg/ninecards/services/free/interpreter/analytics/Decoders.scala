package com.fortysevendeg.ninecards.services.free.interpreter.analytics

import cats.data.Xor
import com.fortysevendeg.ninecards.services.free.domain.rankings.{ DateRange, RankingError }
import com.fortysevendeg.ninecards.services.common.XorDecoder
import enumeratum.{ Circe ⇒ CirceEnum }
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.{ Decoder, Encoder, Json, JsonObject }

object Encoders {
  import model._
  import order._

  implicit object dateRange extends Encoder[DateRange] {
    import org.joda.time.format.DateTimeFormat
    private[this] val dayFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

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

  implicit val responseBodyError: Decoder[RankingError Xor ResponseBody] =
    XorDecoder.xorDecoder[RankingError, ResponseBody]

}