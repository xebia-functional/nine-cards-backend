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
package cards.nine.services.free.interpreter.firebase

import cards.nine.domain.account.DeviceToken
import cards.nine.domain.application.Package
import cards.nine.services.free.domain.Firebase._
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.generic.semiauto._
import io.circe.{ Decoder, Encoder }
import org.http4s.{ EntityDecoder, EntityEncoder }
import org.http4s.circe._

object Decoders {

  implicit val packageD: Decoder[Package] = Decoder.decodeString map Package

  implicit val notificationIndividualResultDecoder: Decoder[NotificationIndividualResult] =
    deriveDecoder[NotificationIndividualResult]

  implicit val notificationResponseDecoder: Decoder[NotificationResponse] =
    deriveDecoder[NotificationResponse]

  implicit val notificationResponseEntityDecoder: EntityDecoder[NotificationResponse] =
    jsonOf(notificationResponseDecoder)
}

object Encoders {

  implicit val packageE: Encoder[Package] = Encoder.encodeString.contramap(_.value)

  implicit val deviceTokenE: Encoder[DeviceToken] = Encoder.encodeString.contramap(_.value)

  implicit val updateCollectionNotificationPayloadEncoder: Encoder[UpdateCollectionNotificationPayload] =
    deriveEncoder[UpdateCollectionNotificationPayload]

  implicit def sendNotificationPayloadEncoder[T: DerivedObjectEncoder]: Encoder[SendNotificationPayload[T]] =
    deriveEncoder[SendNotificationPayload[T]]

  implicit def sendNotificationRequestEncoder[T: DerivedObjectEncoder]: Encoder[SendNotificationRequest[T]] =
    deriveEncoder[SendNotificationRequest[T]]

  implicit val sendNotificationRequestEntityEncoder: EntityEncoder[SendNotificationRequest[UpdateCollectionNotificationPayload]] =
    jsonEncoderOf(sendNotificationRequestEncoder[UpdateCollectionNotificationPayload])

}
