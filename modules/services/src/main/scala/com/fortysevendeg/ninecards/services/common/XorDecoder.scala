package com.fortysevendeg.ninecards.services.common

import cats.data.Xor
import io.circe.Decoder

object XorDecoder {

  /**
    * XorTryDecoder[L,R] gives a decoder for Xor[L,R], where L  represents decoding errors.
    *  It tries first to use the decoder for R, and if it fails it uses the decoder for L.
    *
    *  This is useful for web-through requests: in error conditions (NotFound or InternalServerError),
    *  the body is a string describing the problem.
    */
  def xorDecoder[L, R](implicit decoderLeft: Decoder[L], decoderRight: Decoder[R]): Decoder[L Xor R] =
    decoderRight.map(success ⇒ Xor.Right(success)).or(
      decoderLeft.map(error ⇒ Xor.left(error))
    )

}

