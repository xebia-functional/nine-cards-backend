package cards.nine.api.rankings

import cards.nine.api.rankings.messages._
import cards.nine.domain.application.Package
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

  implicit val reloadRankingRequest: Decoder[Reload.Request] =
    deriveDecoder[Reload.Request]

}

object Encoders {

  implicit val dateTime: Encoder[DateTime] = {
    import org.joda.time.format.DateTimeFormat
    val dayFormatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC
    Encoder.encodeString.contramap(dayFormatter.print)
  }

  implicit val packageName: Encoder[Package] =
    Encoder.encodeString.contramap(_.value)

  implicit val ranking: ObjectEncoder[Ranking] = deriveEncoder[Ranking]

  implicit val error: ObjectEncoder[Reload.Error] = deriveEncoder[Reload.Error]

  implicit val reloadRankingRequest: ObjectEncoder[Reload.Request] =
    deriveEncoder[Reload.Request]
  implicit val reloadRankingResponse: ObjectEncoder[Reload.Response] =
    deriveEncoder[Reload.Response]

}
