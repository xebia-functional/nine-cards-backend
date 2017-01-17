package cards.nine.services.free.interpreter.firebase

import cards.nine.commons.config.Domain.GoogleFirebaseConfiguration
import cards.nine.commons.NineCardsErrors._
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.catscalaz.TaskInstances._
import cards.nine.services.free.algebra.Firebase._
import cards.nine.services.free.domain.Firebase._
import cards.nine.services.free.interpreter.firebase.Decoders._
import cards.nine.services.free.interpreter.firebase.Encoders._
import cats.instances.all._
import cats.syntax.either._
import cats.syntax.traverse._
import cats.~>
import org.http4s.Http4s._
import org.http4s.Uri.{ Authority, RegName }
import org.http4s._
import org.http4s.client.UnexpectedStatus

import scalaz.concurrent.Task

class Services(config: GoogleFirebaseConfiguration) extends (Ops ~> Task) {

  private[this] val client = org.http4s.client.blaze.PooledHttp1Client()

  private[this] def authHeaders: Headers =
    Headers(
      Header("Content-Type", "application/json"),
      Header("Authorization", s"key=${config.authorizationKey}")
    )

  private[this] val uri = Uri(
    scheme    = Option(config.protocol.ci),
    authority = Option(Authority(host = RegName(config.host), port = config.port)),
    path      = config.paths.sendNotification
  )

  private[this] val baseRequest = Request(Method.POST, uri = uri, headers = authHeaders)

  def sendUpdatedCollectionNotification(info: UpdatedCollectionNotificationInfo): Task[Result[SendNotificationResponse]] = {

    def toSendNotificationResponse(responses: List[NotificationResponse]) = SendNotificationResponse(
      multicastIds = responses.map(_.multicast_id),
      success      = responses.map(_.success).sum,
      failure      = responses.map(_.failure).sum,
      canonicalIds = responses.map(_.canonical_ids).sum,
      results      = responses.flatMap(_.results.toList.flatten)
    )

    def doRequest(request: SendNotificationRequest[UpdateCollectionNotificationPayload]) = {

      def toNineCardsError(status: Status) = status match {
        case Status.BadRequest ⇒ HttpBadRequest("Bad request while sending notifications")
        case Status.Unauthorized ⇒ HttpUnauthorized("Wrong credentials while sending notifications")
        case _ ⇒ FirebaseServerError("Unexpected error while sending notifications")
      }

      val httpRequest: Task[Request] =
        baseRequest.withBody[SendNotificationRequest[UpdateCollectionNotificationPayload]](request)

      client.expect[NotificationResponse](httpRequest)
        .map(Either.right)
        .handle {
          case e: UnexpectedStatus ⇒ Either.left(toNineCardsError(e.status))
        }
    }

    val notificationRequests = info.deviceTokens.grouped(1000) map { deviceTokens ⇒
      SendNotificationRequest(
        registration_ids = deviceTokens,
        data             = SendNotificationPayload(
          payloadType = "sharedCollection",
          payload     = UpdateCollectionNotificationPayload(
            publicIdentifier = info.publicIdentifier,
            addedPackages    = info.packagesName
          )
        )
      )
    }

    notificationRequests
      .toList
      .traverse(doRequest)
      .map(responses ⇒ responses.sequenceU.map(toSendNotificationResponse))
  }

  def apply[A](fa: Ops[A]): Task[A] = fa match {
    case SendUpdatedCollectionNotification(info) ⇒ sendUpdatedCollectionNotification(info)
  }
}

object Services {
  def services(config: GoogleFirebaseConfiguration) = new Services(config)
}