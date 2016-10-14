package cards.nine.api

import cards.nine.api.messages.rankings
import cards.nine.domain.application.{ Category, Package }
import enumeratum.{ Circe ⇒ CirceEnum }
import io.circe.generic.semiauto._
import io.circe.{ Decoder, Encoder, ObjectEncoder }
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

  implicit val packageName: Encoder[Package] =
    Encoder.encodeString.contramap(_.value)

  implicit val category: Encoder[Category] = CirceEnum.encoder(Category)

  implicit val catRanking: ObjectEncoder[rankings.CategoryRanking] = deriveEncoder[rankings.CategoryRanking]

  implicit val ranking: ObjectEncoder[rankings.Ranking] = deriveEncoder[rankings.Ranking]

  implicit val error: ObjectEncoder[rankings.Reload.Error] = deriveEncoder[rankings.Reload.Error]

  implicit val reloadRankingRequest: ObjectEncoder[rankings.Reload.Request] =
    deriveEncoder[rankings.Reload.Request]
  implicit val reloadRankingResponse: ObjectEncoder[rankings.Reload.Response] =
    deriveEncoder[rankings.Reload.Response]

}
