package com.fortysevendeg.ninecards.services.free.interpreter.googleplay

import cats.data.Xor
import com.fortysevendeg.ninecards.services.common.XorDecoder
import com.fortysevendeg.ninecards.services.free.domain.GooglePlay._
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import org.http4s.{ EntityDecoder, EntityEncoder }
import org.http4s.circe._

object Decoders {

  implicit val resolveOneEntityDecoder: EntityDecoder[Xor[UnresolvedApp, AppInfo]] =
    jsonOf[UnresolvedApp Xor AppInfo](XorDecoder.xorDecoder[UnresolvedApp, AppInfo])

  implicit val appsCardsEntityDecoder: EntityDecoder[AppsInfo] =
    jsonOf[AppsInfo](deriveDecoder[AppsInfo])
}

object Encoders {

  implicit val packageListEncoder: Encoder[PackageList] = deriveEncoder[PackageList]

  implicit val packageListEntityEncoder: EntityEncoder[PackageList] =
    jsonEncoderOf(packageListEncoder)

}
