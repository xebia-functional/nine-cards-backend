package cards.nine.services.free.interpreter.ranking

import cards.nine.commons.redis.{ Format, Readers, Writers }
import cards.nine.domain.application.Package
import cards.nine.services.free.domain.Ranking._
import io.circe.{ Decoder, Encoder }
import scredis.serialization.{ Reader, Writer }

object Coders {

  import io.circe.generic.auto._
  import io.circe.generic.semiauto._

  val packageD: Decoder[Package] = Decoder.decodeString map Package
  val packageE: Encoder[Package] = Encoder.encodeString.contramap(_.value)

  val rankingD: Decoder[GoogleAnalyticsRanking] = deriveDecoder[GoogleAnalyticsRanking]
  val rankingE: Encoder[GoogleAnalyticsRanking] = deriveEncoder[GoogleAnalyticsRanking]

  implicit val cacheValReader: Reader[Option[CacheVal]] = Readers.decoder(implicitly[Decoder[CacheVal]])

  implicit val cacheValWriter: Writer[CacheVal] = Writers.encoder(implicitly[Encoder[CacheVal]])

  implicit val cacheKeyFormat: Format[CacheKey] = Format(implicitly[Encoder[CacheKey]])

}
