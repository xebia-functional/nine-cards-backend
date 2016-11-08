package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.oauth._
import cats.free.{ :<: }

object GoogleOAuth {

  sealed trait Ops[A]

  case class FetchAccessToken(credentials: ServiceAccount)
    extends Ops[Result[AccessToken]]

  class Services[F[_]](implicit I: Ops :<: F) {

    def fetchAcessToken(serviceAccount: ServiceAccount): NineCardsService[F, AccessToken] =
      NineCardsService(FetchAccessToken(serviceAccount))
  }

  object Services {
    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] = new Services
  }

}