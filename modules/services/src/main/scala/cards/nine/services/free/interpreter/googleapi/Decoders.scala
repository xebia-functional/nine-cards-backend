package cards.nine.services.free.interpreter.googleapi

import cards.nine.services.common.EitherDecoder
import cards.nine.services.free.domain.{ TokenInfo, WrongTokenInfo }
import io.circe.Decoder
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe._

object Decoders {

  implicit val tokenInfoDecoder: Decoder[WrongTokenInfo Either TokenInfo] =
    EitherDecoder.eitherDecoder[WrongTokenInfo, TokenInfo]

  implicit val tokenInfoEntityDecoder: EntityDecoder[WrongTokenInfo Either TokenInfo] =
    jsonOf[WrongTokenInfo Either TokenInfo](tokenInfoDecoder)

}
