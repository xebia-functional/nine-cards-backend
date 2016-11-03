package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.services.free.domain.Firebase._
import cats.free.:<:

object Firebase {

  sealed trait Ops[A]

  case class SendUpdatedCollectionNotification(
    info: UpdatedCollectionNotificationInfo
  ) extends Ops[Result[SendNotificationResponse]]

  class Services[F[_]](implicit I: Ops :<: F) {

    def sendUpdatedCollectionNotification(
      info: UpdatedCollectionNotificationInfo
    ): NineCardsService[F, SendNotificationResponse] =
      NineCardsService(SendUpdatedCollectionNotification(info))

  }

  object Services {

    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] = new Services

  }

}
