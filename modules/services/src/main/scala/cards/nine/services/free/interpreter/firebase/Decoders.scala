package cards.nine.services.free.interpreter.firebase

import cards.nine.services.free.domain.Firebase._
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.generic.semiauto._
import io.circe.{ Decoder, Encoder }
import org.http4s.{ EntityDecoder, EntityEncoder }
import org.http4s.circe._

object Decoders {

  implicit val firebaseErrorDecoder: Decoder[FirebaseError] =
    deriveDecoder[FirebaseError]

  implicit val notificationIndividualResultDecoder: Decoder[NotificationIndividualResult] =
    deriveDecoder[NotificationIndividualResult]

  implicit val notificationResponseDecoder: Decoder[NotificationResponse] =
    deriveDecoder[NotificationResponse]

  implicit val notificationResponseEntityDecoder: EntityDecoder[NotificationResponse] =
    jsonOf(notificationResponseDecoder)
}

object Encoders {

  implicit val updateCollectionNotificationPayloadEncoder: Encoder[UpdateCollectionNotificationPayload] =
    deriveEncoder[UpdateCollectionNotificationPayload]

  implicit def sendNotificationPayloadEncoder[T: DerivedObjectEncoder]: Encoder[SendNotificationPayload[T]] =
    deriveEncoder[SendNotificationPayload[T]]

  implicit def sendNotificationRequestEncoder[T: DerivedObjectEncoder]: Encoder[SendNotificationRequest[T]] =
    deriveEncoder[SendNotificationRequest[T]]

  implicit val sendNotificationRequestEntityEncoder: EntityEncoder[SendNotificationRequest[UpdateCollectionNotificationPayload]] =
    jsonEncoderOf(sendNotificationRequestEncoder[UpdateCollectionNotificationPayload])

}
