package cards.nine.services.free.domain

import cards.nine.domain.account.DeviceToken
import cards.nine.domain.application.Package

object Firebase {

  case class UpdatedCollectionNotificationInfo(
    deviceTokens: List[DeviceToken],
    publicIdentifier: String,
    packagesName: List[Package]
  )

  case class SendNotificationRequest[T](
    registration_ids: List[DeviceToken],
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

  case class SendNotificationResponse(
    multicastIds: List[Long],
    success: Int,
    failure: Int,
    canonicalIds: Int,
    results: List[NotificationIndividualResult]
  )

  object SendNotificationResponse {
    val emptyResponse = SendNotificationResponse(Nil, 0, 0, 0, Nil)
  }

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
