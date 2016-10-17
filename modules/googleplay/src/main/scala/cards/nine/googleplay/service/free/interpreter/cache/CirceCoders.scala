package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.domain.application.{ FullCard, Package }
import enumeratum.{ Circe ⇒ CirceEnum }
import io.circe.{ Decoder, Encoder }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object CirceCoders {

  import io.circe.generic.semiauto._

  private[this] val dateFormatter = DateTimeFormat.forPattern("yyMMddHHmmssSSS").withZoneUTC

  implicit val dateD: Decoder[DateTime] = Decoder.decodeString.map(str ⇒ DateTime.parse(str, dateFormatter))
  implicit val dateE: Encoder[DateTime] = Encoder.encodeString.contramap(dateFormatter.print)

  implicit val keyTypeD: Decoder[KeyType] = CirceEnum.decoder(KeyType)
  implicit val keyTypeE: Encoder[KeyType] = CirceEnum.encoder(KeyType)

  implicit val packageD: Decoder[Package] = Decoder.decodeString.map(Package.apply)
  implicit val packageE: Encoder[Package] = Encoder.encodeString.contramap(_.value)

  implicit val fullCardD: Decoder[FullCard] = deriveDecoder[FullCard]
  implicit val fullCardE: Encoder[FullCard] = deriveEncoder[FullCard]

  implicit val cacheKeyD: Decoder[CacheKey] = deriveDecoder[CacheKey]
  implicit val cacheKeyE: Encoder[CacheKey] = deriveEncoder[CacheKey]

  implicit val cacheValD: Decoder[CacheVal] = Decoder.decodeOption[FullCard].map(CacheVal.apply)
  implicit val cacheValE: Encoder[CacheVal] = Encoder.encodeOption[FullCard].contramap(_.card)

}
