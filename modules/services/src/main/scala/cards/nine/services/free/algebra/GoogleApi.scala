package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService.{ NineCardsService, Result }
import cards.nine.domain.account.GoogleIdToken
import cards.nine.services.free.domain.TokenInfo
import cats.free.:<:

object GoogleApi {

  sealed trait Ops[A]

  case class GetTokenInfo(tokenId: GoogleIdToken) extends Ops[Result[TokenInfo]]

  class Services[F[_]](implicit I: Ops :<: F) {

    def getTokenInfo(tokenId: GoogleIdToken): NineCardsService[F, TokenInfo] =
      NineCardsService(GetTokenInfo(tokenId))
  }

  object Services {

    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] = new Services
  }
}
