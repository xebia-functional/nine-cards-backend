package cards.nine.services.free.domain

import cards.nine.domain.application.Package

object Firebase {

  sealed trait FirebaseError

  case object FirebaseBadRequest extends FirebaseError

  case object FirebaseNotFound extends FirebaseError

  case object FirebaseUnauthorized extends FirebaseError

  case class UpdatedCollectionNotificationInfo(
    deviceTokens: List[String],
    publicIdentifier: String,
    packagesName: List[Package]
  )

  case class SendNotificationRequest[T](
    registration_ids: List[String],
    data: SendNotificationPayload[T]
  )

  case class SendNotificationPayload[T](
    payloadType: String,
    payload: T
  )

  case class UpdateCollectionNotificationPayload(
    publicIdentifier: String,
    addedPackages: List[Package]
  )

  case class NotificationResponse(
    multicast_id: Long,
    success: Int,
    failure: Int,
    canonical_ids: Int,
    results: Option[List[NotificationIndividualResult]]
  )

  case class NotificationIndividualResult(
    message_id: Option[String],
    registration_id: Option[String],
    error: Option[String]
  )

}
