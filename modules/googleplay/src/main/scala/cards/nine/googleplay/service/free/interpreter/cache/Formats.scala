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
package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.commons.redis.{ Format, Readers, Writers }
import cards.nine.domain.application.{ FullCard, Package, PackageRegex }
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.util.Try
import scredis.serialization.{ Reader, Writer }

object Formats {

  implicit val packageD: Decoder[Package] = Decoder.decodeString map Package
  implicit val packageE: Encoder[Package] = Encoder.encodeString.contramap(_.value)

  implicit val fullCardD: Decoder[FullCard] = deriveDecoder[FullCard]
  implicit val fullCardE: Encoder[FullCard] = deriveEncoder[FullCard]

  implicit val cacheValD: Decoder[CacheVal] = Decoder.decodeOption[FullCard].map(CacheVal.apply)
  implicit val cacheValE: Encoder[CacheVal] = Encoder.encodeOption[FullCard].contramap(_.card)

  implicit val valReader: Reader[Option[CacheVal]] = Readers.decoder(cacheValD)
  implicit val valWriter: Writer[CacheVal] = Writers.encoder(cacheValE)

  // Cache Key Format: package name, colon, key type
  private[this] val cacheKeyRegex = """([a-zA-Z0-9\.\_]+):([a-zA-Z]+)""".r

  def formatKey(key: CacheKey): String =
    s"${key.`package`.value}:${key.keyType.entryName}"

  def parseKey(keyStr: String): Option[CacheKey] =
    for {
      List(packStr, typeStr) ← cacheKeyRegex.unapplySeq(keyStr)
      keyType ← KeyType.withNameOption(typeStr)
    } yield CacheKey(Package(packStr), keyType)

  implicit val keyFormat: Format[CacheKey] = Format(formatKey _)

  private[this] val dateFormatter = DateTimeFormat.forPattern("yyMMddHHmmssSSS").withZoneUTC
  def parseDate(str: String): Option[DateTime] = Try(dateFormatter.parseDateTime(str)).toOption
  def formatDate(date: DateTime): String = dateFormatter.print(date)

  implicit val dateReader: Reader[Option[DateTime]] = Readers.parser(parseDate)

  implicit val dateWriter: Writer[DateTime] = Writers.printer(formatDate)

  private[this] val pendingQueueCode = "pending_packages"

  implicit val pendingKeyFormat: Format[PendingQueueKey.type] = Format(_key ⇒ pendingQueueCode)
  implicit val packReader: Reader[Option[Package]] = Readers.parser(PackageRegex.parse)
  implicit val packWriter: Writer[Package] = Writers.printer(p ⇒ p.value)

}

