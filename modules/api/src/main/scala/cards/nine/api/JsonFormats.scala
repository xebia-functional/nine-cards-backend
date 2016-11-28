package cards.nine.api

import cards.nine.api.messages.GooglePlayMessages._
import cards.nine.api.messages.InstallationsMessages._
import cards.nine.api.messages.UserMessages._
import cards.nine.domain.application.{ Package, Widget }
import cards.nine.domain.account._
import spray.httpx.SprayJsonSupport
import spray.json._

trait JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

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

  implicit val apiCategorizeAppsRequestFormat = jsonFormat1(ApiAppsInfoRequest)

  implicit val apiCategorizedAppFormat = jsonFormat2(ApiCategorizedApp)

  implicit val apiIconAppFormat = jsonFormat3(ApiIconApp)

  implicit def apiAppsInfoResponse[A](implicit base: JsonFormat[A]): RootJsonFormat[ApiAppsInfoResponse[A]] =
    jsonFormat2(ApiAppsInfoResponse[A])

  implicit val appGooglePlayInfoFormat = jsonFormat7(ApiDetailsApp)

  implicit val apiSetAppInfoRequestFormat = jsonFormat7(ApiSetAppInfoRequest)
  implicit val apiSetAppInfoResponseFormat = jsonFormat0(ApiSetAppInfoResponse)

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