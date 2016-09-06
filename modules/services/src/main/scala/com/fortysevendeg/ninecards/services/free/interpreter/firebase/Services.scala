package com.fortysevendeg.ninecards.services.free.interpreter.firebase

import cats.data.Xor
import com.fortysevendeg.ninecards.services.free.domain.Firebase._
import com.fortysevendeg.ninecards.services.free.interpreter.firebase.Decoders._
import com.fortysevendeg.ninecards.services.free.interpreter.firebase.Encoders._
import org.http4s.Http4s._
import org.http4s.Uri.{ Authority, RegName }
import org.http4s._
import org.http4s.client.UnexpectedStatus

import scalaz.concurrent.Task

class Services(config: Configuration) {

  private[this] val client = org.http4s.client.blaze.PooledHttp1Client()

  private[this] def authHeaders: Headers =
    Headers(
      Header("Content-Type", "application/json"),
      Header("Authorization", s"key=${config.authorizationKey}")
    )

  private[this] val uri = Uri(
    scheme    = Option(config.protocol.ci),
    authority = Option(Authority(host = RegName(config.host), port = config.port)),
    path      = config.sendNotificationPath
  )

  private[this] val baseRequest = Request(Method.POST, uri = uri, headers = authHeaders)

  def sendUpdatedCollectionNotification(
    info: UpdatedCollectionNotificationInfo
  ): Task[FirebaseError Xor NotificationResponse] = {

    val notificationRequest = SendNotificationRequest(
      registration_ids = info.deviceTokens,
      data             = SendNotificationPayload(
        payloadType = "sharedCollection",
        payload     = UpdateCollectionNotificationPayload(
          publicIdentifier = info.publicIdentifier,
          addedPackages    = info.packagesName
        )
      )
    )

    val request = baseRequest
      .withBody[SendNotificationRequest[UpdateCollectionNotificationPayload]](notificationRequest)

    client.expect[NotificationResponse](request)
      .map(Xor.right[FirebaseError, NotificationResponse])
      .handle {
        case e: UnexpectedStatus ⇒ e.status match {
          case Status.BadRequest ⇒ Xor.left(FirebaseBadRequest)
          case Status.Unauthorized ⇒ Xor.left(FirebaseUnauthorized)
        }
      }
  }
}

object Services {
  def services(implicit config: Configuration) = new Services(config)
}