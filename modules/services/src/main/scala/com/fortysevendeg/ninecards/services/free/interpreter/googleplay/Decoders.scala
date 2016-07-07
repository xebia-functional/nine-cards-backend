package com.fortysevendeg.ninecards.services.free.interpreter.googleplay

import cats.data.Xor
import com.fortysevendeg.ninecards.services.common.XorDecoder
import com.fortysevendeg.ninecards.services.free.domain.GooglePlay._
import io.circe.{ Decoder, Encoder }
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import org.http4s.EntityEncoder
import org.http4s.circe._

object Decoders {

  implicit val resolveManyEntityDecoder = jsonOf[Failure Xor AppsDetails](
    XorDecoder.xorDecoder[Failure, AppsDetails]
  )

  implicit val resolveOneEntityDecoder = jsonOf[Failure Xor App](
    XorDecoder.xorDecoder[Failure, App]
  )
}

object Encoders {

  implicit val packageListEncoder: Encoder[PackageListRequest] = deriveEncoder[PackageListRequest]

  implicit val packageListEntityEncoder: EntityEncoder[PackageListRequest] = jsonEncoderOf(packageListEncoder)

}

