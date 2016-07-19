package com.fortysevendeg.ninecards.api

import cats.data.Xor
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.api.messages.UserMessages._
import com.fortysevendeg.ninecards.processes.domain._
import io.circe.{ Decoder, Encoder, Json }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import spray.httpx.SprayJsonSupport
import spray.json._

trait JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

  implicit object JodaDateTimeFormat extends RootJsonFormat[DateTime] {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val dateExample = formatter.print(0)

    def error(v: String) = deserializationError(
      s"'$v' is not a valid date value. The format for dates must be: '$dateExample'"
    )

    val decodeDateTime: Decoder[DateTime] = Decoder.instance { cursor ⇒
      cursor.as[String].flatMap {
        dateTime ⇒ Xor.right(DateTime.parse(dateTime, formatter))
      }
    }

    val encodeDateTime: Encoder[DateTime] = Encoder.instance { dateTime: DateTime ⇒
      Json.fromString(formatter.print(dateTime))
    }

    def write(obj: DateTime): JsValue = JsString(encodeDateTime(obj).noSpaces)

    def read(json: JsValue): DateTime = json match {
      case JsString(s) ⇒ decodeDateTime(Json.fromString(s).hcursor).fold(error(s), d ⇒ d)
      case _ ⇒ error(json.toString)
    }

  }

  implicit val googlePlayAppFormat = jsonFormat7(GooglePlayApp)

  implicit val apiLoginRequestFormat = jsonFormat3(ApiLoginRequest)

  implicit val apiLoginResponseFormat = jsonFormat2(ApiLoginResponse)

  implicit val updateInstallationRequestFormat = jsonFormat1(ApiUpdateInstallationRequest)

  implicit val updateInstallationResponseFormat = jsonFormat2(ApiUpdateInstallationResponse)

  implicit val apiResolvedPackageInfoFormat = jsonFormat7(ApiResolvedPackageInfo)

  implicit val apiGetCollectionByPublicIdentifierResponseFormat = jsonFormat13(ApiSharedCollection)

  implicit val apiCreateCollectionRequestFormat = jsonFormat9(ApiCreateCollectionRequest)

  implicit val apiCreateCollectionResponseFormat = jsonFormat12(ApiCreateCollectionResponse)

  implicit val apiSubscribeResponseFormat = jsonFormat0(ApiSubscribeResponse)

  implicit val apiUnsubscribeResponseFormat = jsonFormat0(ApiUnsubscribeResponse)

}

object JsonFormats extends JsonFormats