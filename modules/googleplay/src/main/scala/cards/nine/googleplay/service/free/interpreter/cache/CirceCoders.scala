package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.commons.redis.CacheQueue
import cards.nine.domain.application.{ FullCard, Package }
import com.redis.RedisClient
import com.redis.serialization.{ Format, Parse }
import io.circe.{ Decoder, Encoder }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.util.Try

object CirceCoders {

  import io.circe.generic.semiauto._

  implicit val packageD: Decoder[Package] = Decoder.decodeString map Package
  implicit val packageE: Encoder[Package] = Encoder.encodeString.contramap(_.value)

  implicit val fullCardD: Decoder[FullCard] = deriveDecoder[FullCard]
  implicit val fullCardE: Encoder[FullCard] = deriveEncoder[FullCard]

  implicit val cacheValD: Decoder[CacheVal] = Decoder.decodeOption[FullCard].map(CacheVal.apply)
  implicit val cacheValE: Encoder[CacheVal] = Encoder.encodeOption[FullCard].contramap(_.card)

}

/**
  * This object contains all the formatting and parsing for Cache Keys.
  */
object KeyFormat {

  // Cache Key Format: package name, colon, key type
  private[this] val cacheKeyRegex = """([a-zA-Z0-9\.\_]+):([a-zA-Z]+)""".r

  def format(key: CacheKey): String =
    s"${key.`package`.value}:${key.keyType.entryName}"

  def parse(keyStr: String): Option[CacheKey] =
    for {
      List(packStr, typeStr) ← cacheKeyRegex.unapplySeq(keyStr)
      keyType ← KeyType.withNameOption(typeStr)
    } yield CacheKey(Package(packStr), keyType)

}

object ErrorCache {

  private[this] val dateFormatter = DateTimeFormat.forPattern("yyMMddHHmmssSSS").withZoneUTC
  def parseDate(str: String): Option[DateTime] = Try(dateFormatter.parseDateTime(str)).toOption
  def formatDate(date: DateTime): String = dateFormatter.print(date)

  private[this] final val keyAndDateFormat: Format = Format {
    case key: CacheKey ⇒ KeyFormat.format(key)
    case date: DateTime ⇒ formatDate(date)
  }

  private[this] final val dateParse: Parse[Option[DateTime]] =
    Parse(bv ⇒ parseDate(Parse.Implicits.parseString(bv)))

  def apply(client: RedisClient): CacheQueue[CacheKey, DateTime] =
    new CacheQueue[CacheKey, DateTime](client)(keyAndDateFormat, dateParse)

}

object PendingQueue {

  object QueueKey

  final val pendingQueueCode = "pending_packages"

  private[this] val packageRegex = """[a-zA-Z0-9\.\_]+""".r

  private[this] def parsePackage(value: String): Option[Package] =
    packageRegex.unapplySeq(value).map(_ ⇒ Package(value))

  private[this] val format: Format = Format {
    case QueueKey ⇒ pendingQueueCode
    case pack: Package ⇒ pack.value
  }

  private[this] val parse: Parse[Option[Package]] =
    Parse(bv ⇒ parsePackage(Parse.Implicits.parseString(bv)))

  def apply(client: RedisClient): CacheQueue[QueueKey.type, Package] =
    new CacheQueue(client)(format, parse)

}

