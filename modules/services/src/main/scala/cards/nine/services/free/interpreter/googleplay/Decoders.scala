package cards.nine.services.free.interpreter.googleplay

import cats.data.Xor
import cards.nine.services.common.XorDecoder
import cards.nine.services.free.domain.GooglePlay._
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import org.http4s.{ EntityDecoder, EntityEncoder }
import org.http4s.circe._

object Decoders {

  implicit val resolveOneEntityDecoder: EntityDecoder[Xor[String, AppInfo]] =
    jsonOf[String Xor AppInfo](XorDecoder.xorDecoder[String, AppInfo])

  implicit val appsCardsEntityDecoder: EntityDecoder[AppsInfo] =
    jsonOf[AppsInfo](deriveDecoder[AppsInfo])

  implicit val recommendationsEntityDecoder: EntityDecoder[Recommendations] =
    jsonOf[Recommendations](deriveDecoder[Recommendations])
}

object Encoders {

  implicit val packageListEncoder: Encoder[PackageList] = deriveEncoder[PackageList]

  implicit val packageListEntityEncoder: EntityEncoder[PackageList] =
    jsonEncoderOf(packageListEncoder)

}
