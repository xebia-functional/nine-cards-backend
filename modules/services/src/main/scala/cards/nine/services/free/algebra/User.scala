package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.account._
import cards.nine.services.free.domain
import cats.free.Inject

object User {

  sealed trait Ops[A]

  case class Add(email: Email, apiKey: ApiKey, sessionToken: SessionToken) extends Ops[Result[domain.User]]

  case class AddInstallation(user: Long, deviceToken: Option[DeviceToken], androidId: AndroidId) extends Ops[Result[domain.Installation]]

  case class GetByEmail(email: Email) extends Ops[Result[domain.User]]

  case class GetBySessionToken(sessionToken: SessionToken) extends Ops[Result[domain.User]]

  case class GetInstallationByUserAndAndroidId(user: Long, androidId: AndroidId) extends Ops[Result[domain.Installation]]

  case class GetSubscribedInstallationByCollection(collectionPublicId: String) extends Ops[Result[List[domain.Installation]]]

  case class UpdateInstallation(user: Long, deviceToken: Option[DeviceToken], androidId: AndroidId) extends Ops[Result[domain.Installation]]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def add(email: Email, apiKey: ApiKey, sessionToken: SessionToken): NineCardsService[F, domain.User] =
      NineCardsService(Add(email, apiKey, sessionToken))

    def addInstallation(user: Long, deviceToken: Option[DeviceToken], androidId: AndroidId): NineCardsService[F, domain.Installation] =
      NineCardsService(AddInstallation(user, deviceToken, androidId))

    def getByEmail(email: Email): NineCardsService[F, domain.User] =
      NineCardsService(GetByEmail(email))

    def getBySessionToken(sessionToken: SessionToken): NineCardsService[F, domain.User] =
      NineCardsService(GetBySessionToken(sessionToken))

    def getInstallationByUserAndAndroidId(user: Long, androidId: AndroidId): NineCardsService[F, domain.Installation] =
      NineCardsService(GetInstallationByUserAndAndroidId(user, androidId))

    def getSubscribedInstallationByCollection(collectionPublicId: String): NineCardsService[F, List[domain.Installation]] =
      NineCardsService(GetSubscribedInstallationByCollection(collectionPublicId))

    def updateInstallation(user: Long, deviceToken: Option[DeviceToken], androidId: AndroidId): NineCardsService[F, domain.Installation] =
      NineCardsService(UpdateInstallation(user, deviceToken, androidId))
  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] = new Services

  }

}
