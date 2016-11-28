package cards.nine.api.collections

import cats.data.Xor
import cards.nine.api.collections.messages._
import cards.nine.processes.collections.messages._
import io.circe.{ Decoder, Encoder, Json }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import spray.httpx.SprayJsonSupport
import spray.json._

private[collections] trait JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

  import cards.nine.api.JsonFormats.PackageJsonFormat

  implicit object JodaDateTimeFormat extends RootJsonFormat[DateTime] {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZoneUTC
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

    def write(obj: DateTime): JsValue = encodeDateTime(obj).as[String].fold(
      f ⇒ serializationError(f.message),
      v ⇒ JsString(v)
    )

    def read(json: JsValue): DateTime = json match {
      case JsString(s) ⇒ decodeDateTime(Json.fromString(s).hcursor).fold(_ ⇒ error(s), d ⇒ d)
      case _ ⇒ error(json.toString)
    }

  }

  implicit val appInfoFormat = jsonFormat7(ApiCollectionApp)

  implicit val apiSharedCollection = jsonFormat12(ApiSharedCollection)

  implicit val apiSharedCollectionList = jsonFormat1(ApiSharedCollectionList)

  implicit val apiCreateCollectionRequestFormat = jsonFormat8(ApiCreateCollectionRequest)

  implicit val packagesStatsFormat = jsonFormat2(PackagesStats)

  implicit val apiCreateCollectionResponseFormat = jsonFormat2(ApiCreateOrUpdateCollectionResponse)

  implicit val apiIncreaseViewsCountByOneResponseFormat = jsonFormat1(ApiIncreaseViewsCountByOneResponse)

  implicit val apiSubscribeResponseFormat = jsonFormat0(ApiSubscribeResponse)

  implicit val apiUnsubscribeResponseFormat = jsonFormat0(ApiUnsubscribeResponse)

  implicit val sharedCollectionUpdateInfoFormat = jsonFormat1(SharedCollectionUpdateInfo)

  implicit val apiUpdateCollectionRequestFormat = jsonFormat2(ApiUpdateCollectionRequest)

  implicit val apiGetSubscriptionsByUserResponseFormat = jsonFormat1(ApiGetSubscriptionsByUser)

}

object JsonFormats extends JsonFormats
