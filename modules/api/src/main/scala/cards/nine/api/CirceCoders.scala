package cards.nine.api

import cards.nine.api.messages.rankings
import cards.nine.services.free.domain.Category
import enumeratum.{ Enum, EnumEntry, Circe ⇒ CirceEnum }
import io.circe.{ Decoder, Encoder, Json, ObjectEncoder }
import io.circe.generic.semiauto._
import org.joda.time.DateTime

object Decoders {

  import io.circe.generic.semiauto._

  implicit val dateTime: Decoder[DateTime] = {
    import org.joda.time.format.DateTimeFormat
    val dayFormatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC
    Decoder.decodeString.map(str ⇒ DateTime.parse(str, dayFormatter))
  }

  implicit val reloadRankingRequest: Decoder[rankings.Reload.Request] =
    deriveDecoder[rankings.Reload.Request]

}

object Encoders {

  implicit val dateTime: Encoder[DateTime] = {
    import org.joda.time.format.DateTimeFormat
    val dayFormatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC
    Encoder.encodeString.contramap(dayFormatter.print)
  }

  implicit val category: Encoder[Category] = CirceEnum.encoder(Category)

  implicit val catRanking: ObjectEncoder[rankings.CategoryRanking] = deriveEncoder[rankings.CategoryRanking]

  implicit val ranking: ObjectEncoder[rankings.Ranking] = deriveEncoder[rankings.Ranking]

  implicit val error: ObjectEncoder[rankings.Reload.Error] = deriveEncoder[rankings.Reload.Error]

  implicit val reloadRankingRequest: ObjectEncoder[rankings.Reload.Request] =
    deriveEncoder[rankings.Reload.Request]
  implicit val reloadRankingResponse: ObjectEncoder[rankings.Reload.Response] =
    deriveEncoder[rankings.Reload.Response]

}

