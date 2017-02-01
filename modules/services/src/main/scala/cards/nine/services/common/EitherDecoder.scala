/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.services.common

import cats.syntax.either._
import io.circe.Decoder

object EitherDecoder {

  /**
    * EitherDecoder[L,R] gives a decoder for Either[L,R], where L  represents decoding errors.
    *  It tries first to use the decoder for R, and if it fails it uses the decoder for L.
    *
    *  This is useful for web-through requests: in error conditions (NotFound or InternalServerError),
    *  the body is a string describing the problem.
    */
  def eitherDecoder[L, R](implicit decoderLeft: Decoder[L], decoderRight: Decoder[R]): Decoder[L Either R] =
    decoderRight.map(success ⇒ Either.right(success)).or(
      decoderLeft.map(error ⇒ Either.left(error))
    )

}

