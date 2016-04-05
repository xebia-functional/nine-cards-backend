package com.fortysevendeg.ninecards.services.free.interpreter.impl

import cats.data.Xor
import com.fortysevendeg.ninecards.services.free.domain.{TokenInfo, WrongTokenInfo}
import io.circe.Decoder
import io.circe.generic.auto._
import org.http4s.circe._

trait GoogleApiDecoders {

  implicit val tokenInfoDecoder: Decoder[WrongTokenInfo Xor TokenInfo] =
    Decoder[TokenInfo].map(t ⇒ Xor.right(t)).or(
      Decoder[WrongTokenInfo].map(w ⇒ Xor.left(w))
    )

  implicit val tokenInfoEntityDecoder = jsonOf[WrongTokenInfo Xor TokenInfo](tokenInfoDecoder)

}
