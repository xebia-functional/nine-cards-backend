package cards.nine.services.free.interpreter.googleapi

import cats.data.Xor
import cards.nine.services.common.XorDecoder
import cards.nine.services.free.domain.{ TokenInfo, WrongTokenInfo }
import io.circe.Decoder
import io.circe.generic.auto._
import org.http4s.circe._

object Decoders {

  implicit val tokenInfoDecoder: Decoder[WrongTokenInfo Xor TokenInfo] =
    XorDecoder.xorDecoder[WrongTokenInfo, TokenInfo]

  implicit val tokenInfoEntityDecoder = jsonOf[WrongTokenInfo Xor TokenInfo](tokenInfoDecoder)

}
