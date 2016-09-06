package com.fortysevendeg.ninecards.services.free.algebra

import cats.data.Xor
import cats.free.{ Free, Inject }
import com.fortysevendeg.ninecards.services.free.domain.Firebase._

object Firebase {

  sealed trait Ops[A]

  case class SendUpdatedCollectionNotification(
    info: UpdatedCollectionNotificationInfo
  ) extends Ops[FirebaseError Xor NotificationResponse]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def sendUpdatedCollectionNotification(
      info: UpdatedCollectionNotificationInfo
    ): Free[F, FirebaseError Xor NotificationResponse] =
      Free.inject[Ops, F](SendUpdatedCollectionNotification(info))

  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] = new Services

  }

}
