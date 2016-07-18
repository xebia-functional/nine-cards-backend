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

  implicit val resolveOneEntityDecoder = jsonOf[UnresolvedApp Xor AppCard](
    XorDecoder.xorDecoder[UnresolvedApp, AppCard]
  )

  implicit val appsCardsEntityDecoder = jsonOf[AppsCards](deriveDecoder[AppsCards])
}

object Encoders {

  implicit val packageListEncoder: Encoder[PackageList] = deriveEncoder[PackageList]

  implicit val packageListEntityEncoder: EntityEncoder[PackageList] = jsonEncoderOf(packageListEncoder)

}

