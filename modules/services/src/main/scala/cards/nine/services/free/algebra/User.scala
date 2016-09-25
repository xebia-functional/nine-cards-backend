package cards.nine.services.free.algebra

import cards.nine.services.free.domain
import cats.free.{ Free, Inject }

object User {

  sealed trait Ops[A]

  case class Add(email: String, apiKey: String, sessionToken: String) extends Ops[domain.User]

  case class AddInstallation(user: Long, deviceToken: Option[String], androidId: String) extends Ops[domain.Installation]

  case class GetByEmail(email: String) extends Ops[Option[domain.User]]

  case class GetBySessionToken(sessionToken: String) extends Ops[Option[domain.User]]

  case class GetInstallationByUserAndAndroidId(user: Long, androidId: String) extends Ops[Option[domain.Installation]]

  case class GetSubscribedInstallationByCollection(collectionPublicId: String) extends Ops[List[domain.Installation]]

  case class UpdateInstallation(user: Long, deviceToken: Option[String], androidId: String) extends Ops[domain.Installation]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def add(email: String, apiKey: String, sessionToken: String): Free[F, domain.User] =
      Free.inject[Ops, F](Add(email, apiKey, sessionToken))

    def addInstallation(user: Long, deviceToken: Option[String], androidId: String): Free[F, domain.Installation] =
      Free.inject[Ops, F](AddInstallation(user, deviceToken, androidId))

    def getByEmail(email: String): Free[F, Option[domain.User]] =
      Free.inject[Ops, F](GetByEmail(email))

    def getBySessionToken(sessionToken: String): Free[F, Option[domain.User]] =
      Free.inject[Ops, F](GetBySessionToken(sessionToken))

    def getInstallationByUserAndAndroidId(user: Long, androidId: String): Free[F, Option[domain.Installation]] =
      Free.inject[Ops, F](GetInstallationByUserAndAndroidId(user, androidId))

    def getSubscribedInstallationByCollection(collectionPublicId: String): Free[F, List[domain.Installation]] =
      Free.inject[Ops, F](GetSubscribedInstallationByCollection(collectionPublicId))

    def updateInstallation(user: Long, deviceToken: Option[String], androidId: String): Free[F, domain.Installation] =
      Free.inject[Ops, F](UpdateInstallation(user, deviceToken, androidId))
  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] = new Services

  }

}
