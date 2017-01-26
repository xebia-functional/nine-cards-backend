package cards.nine.api.applications

import cards.nine.domain.application.{ Package, Widget }
import spray.httpx.SprayJsonSupport
import spray.json._

private[applications] trait JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

  import messages._

  implicit object PackageJsonFormat extends JsonFormat[Package] {
    def read(json: JsValue): Package = Package(StringJsonFormat.read(json))
    def write(pack: Package): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit val apiCategorizeAppsRequestFormat = jsonFormat1(ApiAppsInfoRequest)

  implicit val apiCategorizedAppFormat = jsonFormat2(ApiCategorizedApp)

  implicit val apiIconAppFormat = jsonFormat3(ApiIconApp)

  implicit def apiAppsInfoResponse[A](implicit base: JsonFormat[A]): RootJsonFormat[ApiAppsInfoResponse[A]] =
    jsonFormat2(ApiAppsInfoResponse[A])

  implicit val appGooglePlayInfoFormat = jsonFormat7(ApiDetailsApp)

  implicit val apiSetAppInfoRequestFormat = jsonFormat7(ApiSetAppInfoRequest)
  implicit val apiSetAppInfoResponseFormat = jsonFormat0(ApiSetAppInfoResponse)

  implicit val apiRankedAppsByCategoryFormat = jsonFormat2(ApiRankedAppsByCategory)

  implicit val apiRankAppsRequestFormat = jsonFormat2(ApiRankAppsRequest)
  implicit val apiRankAppsResponseFormat = jsonFormat1(ApiRankAppsResponse)

  implicit val apiRankAppsByMomentsRequestFormat = jsonFormat4(ApiRankByMomentsRequest)

  implicit val apiSearchAppsRequest = jsonFormat3(ApiSearchAppsRequest)

  implicit val googlePlayRecommendationFormat = jsonFormat7(ApiRecommendation)

  implicit val apiSearchAppsResponse = jsonFormat1(ApiSearchAppsResponse)

  implicit val apiGetRecommendationsByCategoryRequestFormat = jsonFormat2(ApiGetRecommendationsByCategoryRequest)

  implicit val apiGetRecommendationsForAppsRequestFormat = jsonFormat4(ApiGetRecommendationsForAppsRequest)

  implicit val apiGetRecommendationsResponseFormat = jsonFormat1(ApiGetRecommendationsResponse)

  implicit val widgetFormat = jsonFormat2(Widget.apply)

  implicit val apiRankedWidgetsByMomentFormat = jsonFormat2(ApiRankedWidgetsByMoment)

  implicit val apiRankWidgetsResponseFormat = jsonFormat1(ApiRankWidgetsResponse)

  implicit val apiCategorizedAppsFormat = jsonFormat3(ApiCategorizedApps)
}

private[applications] object JsonFormats extends JsonFormats
