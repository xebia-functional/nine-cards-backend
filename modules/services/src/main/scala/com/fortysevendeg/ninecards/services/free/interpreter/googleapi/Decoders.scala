package com.fortysevendeg.ninecards.services.free.interpreter.googleapi

import cats.data.Xor
import com.fortysevendeg.ninecards.services.common.XorDecoder
import com.fortysevendeg.ninecards.services.free.domain.{ TokenInfo, WrongTokenInfo }
import io.circe.Decoder
import io.circe.generic.auto._
import org.http4s.circe._

object Decoders {

  implicit val tokenInfoDecoder: Decoder[WrongTokenInfo Xor TokenInfo] =
    XorDecoder.xorDecoder[WrongTokenInfo, TokenInfo]

  implicit val tokenInfoEntityDecoder = jsonOf[WrongTokenInfo Xor TokenInfo](tokenInfoDecoder)

}
