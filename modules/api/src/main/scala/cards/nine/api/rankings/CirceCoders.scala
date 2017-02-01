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
