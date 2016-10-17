package cards.nine.services.free.interpreter.googleapi

import cards.nine.domain.account.GoogleIdToken
import cards.nine.services.free.algebra.GoogleApi._
import cats.data.Xor
import cards.nine.services.free.domain.{ TokenInfo, WrongTokenInfo }
import cats.~>
import org.http4s.Http4s._
import org.http4s.Uri
import org.http4s.Uri.{ Authority, RegName }

import scalaz.concurrent.Task

class Services(config: Configuration) extends (Ops ~> Task) {

  import Decoders._

  val client = org.http4s.client.blaze.PooledHttp1Client()

  def getTokenInfo(tokenId: GoogleIdToken): Task[WrongTokenInfo Xor TokenInfo] = {
    val authority = Authority(host = RegName(config.host), port = config.port)

    val getTokenInfoUri = Uri(scheme = Option(config.protocol.ci), authority = Option(authority))
      .withPath(config.tokenInfoUri)
      .withQueryParam(config.tokenIdQueryParameter, tokenId.value)

    client.expect[WrongTokenInfo Xor TokenInfo](getTokenInfoUri)
  }

  def apply[A](fa: Ops[A]): Task[A] = fa match {
    case GetTokenInfo(tokenId: GoogleIdToken) ⇒ getTokenInfo(tokenId)
  }
}

object Services {

  implicit def services(implicit config: Configuration) = new Services(config)
}
