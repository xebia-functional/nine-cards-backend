package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.domain.application.{ FullCard, Package }
import io.circe.{ Decoder, Encoder }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

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

  import KeyType._

  private[this] val dateFormatter = DateTimeFormat.forPattern("yyMMddHHmmssSSS").withZoneUTC
  private[this] def parseDate(str: String): DateTime = DateTime.parse(str, dateFormatter)
  private[this] def formatDate(date: DateTime): String = dateFormatter.print(date)

  private[this] val regex = """([a-zA-Z0-9\.\_]+):([a-zA-Z0-9\.\_]+)(:[0-9]+)?""".r

  def format(key: CacheKey): String = {
    import key._
    val dateStr = key.date match {
      case None ⇒ ""
      case Some(date) ⇒ s":${formatDate(date)}"
    }
    s"${`package`.value}:${keyType.entryName}$dateStr"
  }

  def parse(keyStr: String): Option[CacheKey] =
    for {
      List(packStr, typeStr, dateNull) ← regex.unapplySeq(keyStr)
      keyType ← KeyType.withNameOption(typeStr)
      date = Option(dateNull).map { str ⇒ parseDate(str.drop(1)) }
    } yield CacheKey(Package(packStr), keyType, date)

  object Pattern {
    val allPending: String = s"*:${Pending.entryName}"
    def errorsFor(pack: Package): String = s"${pack.value}:${Error.entryName}:*"
  }

}
