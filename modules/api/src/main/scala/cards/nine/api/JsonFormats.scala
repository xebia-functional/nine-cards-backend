package cards.nine.api

import cats.data.Xor
import cards.nine.api.messages.GooglePlayMessages._
import cards.nine.api.messages.InstallationsMessages._
import cards.nine.api.messages.SharedCollectionMessages._
import cards.nine.api.messages.UserMessages._
import cards.nine.domain.application.{ Package, Widget }
import cards.nine.domain.account._
import cards.nine.processes.collections.messages._
import io.circe.{ Decoder, Encoder, Json }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import spray.httpx.SprayJsonSupport
import spray.json._

trait JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

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

  implicit object PackageJsonFormat extends JsonFormat[Package] {
    def read(json: JsValue): Package = Package(StringJsonFormat.read(json))
    def write(pack: Package): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit object EmailJsonFormat extends JsonFormat[Email] {
    def read(json: JsValue): Email = Email(StringJsonFormat.read(json))
    def write(pack: Email): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit object DeviceTokenJsonFormat extends JsonFormat[DeviceToken] {
    def read(json: JsValue): DeviceToken = DeviceToken(StringJsonFormat.read(json))
    def write(pack: DeviceToken): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit object ApiKeyJsonFormat extends JsonFormat[ApiKey] {
    def read(json: JsValue): ApiKey = ApiKey(StringJsonFormat.read(json))
    def write(pack: ApiKey): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit object AndroidIdJsonFormat extends JsonFormat[AndroidId] {
    def read(json: JsValue): AndroidId = AndroidId(StringJsonFormat.read(json))
    def write(pack: AndroidId): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit object GoogleIdTokenJsonFormat extends JsonFormat[GoogleIdToken] {
    def read(json: JsValue): GoogleIdToken = GoogleIdToken(StringJsonFormat.read(json))
    def write(pack: GoogleIdToken): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit object SessionTokenJsonFormat extends JsonFormat[SessionToken] {
    def read(json: JsValue): SessionToken = SessionToken(StringJsonFormat.read(json))
    def write(pack: SessionToken): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit val apiLoginRequestFormat = jsonFormat3(ApiLoginRequest)

  implicit val apiLoginResponseFormat = jsonFormat2(ApiLoginResponse)

  implicit val updateInstallationRequestFormat = jsonFormat1(ApiUpdateInstallationRequest)

  implicit val updateInstallationResponseFormat = jsonFormat2(ApiUpdateInstallationResponse)

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

  implicit val apiCategorizeAppsRequestFormat = jsonFormat1(ApiAppsInfoRequest)

  implicit val apiCategorizedAppFormat = jsonFormat2(ApiCategorizedApp)

  implicit val apiIconAppFormat = jsonFormat3(ApiIconApp)

  implicit def apiAppsInfoResponse[A](implicit base: JsonFormat[A]): RootJsonFormat[ApiAppsInfoResponse[A]] =
    jsonFormat2(ApiAppsInfoResponse[A])

  implicit val appGooglePlayInfoFormat = jsonFormat7(ApiDetailsApp)

  implicit val apiSetAppInfoRequestFormat = jsonFormat7(ApiSetAppInfoRequest)
  implicit val apiSetAppInfoResponseFormat = jsonFormat0(ApiSetAppInfoResponse)

  implicit val apiGetSubscriptionsByUserResponseFormat = jsonFormat1(ApiGetSubscriptionsByUser)

  implicit val apiGetRecommendationsByCategoryRequestFormat = jsonFormat2(ApiGetRecommendationsByCategoryRequest)

  implicit val apiGetRecommendationsForAppsRequestFormat = jsonFormat4(ApiGetRecommendationsForAppsRequest)

  implicit val googlePlayRecommendationFormat = jsonFormat7(ApiRecommendation)

  implicit val apiGetRecommendationsResponseFormat = jsonFormat1(ApiGetRecommendationsResponse)

  implicit val apiRankAppsRequestFormat = jsonFormat2(ApiRankAppsRequest)

  implicit val apiRankAppsByMomentsRequestFormat = jsonFormat4(ApiRankByMomentsRequest)

  implicit val apiRankedAppsByCategoryFormat = jsonFormat2(ApiRankedAppsByCategory)

  implicit val apiRankAppsResponseFormat = jsonFormat1(ApiRankAppsResponse)

  implicit val widgetFormat = jsonFormat2(Widget.apply)

  implicit val apiRankedWidgetsByMomentFormat = jsonFormat2(ApiRankedWidgetsByMoment)

  implicit val apiRankWidgetsResponseFormat = jsonFormat1(ApiRankWidgetsResponse)

  implicit val apiSearchAppsRequest = jsonFormat3(ApiSearchAppsRequest)

  implicit val apiSearchAppsResponse = jsonFormat1(ApiSearchAppsResponse)
}

object JsonFormats extends JsonFormats
