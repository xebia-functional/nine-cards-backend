package cards.nine.api

import cards.nine.api.messages.GooglePlayMessages._
import cards.nine.domain.application.{ Package, Widget }
import spray.httpx.SprayJsonSupport
import spray.json._

trait JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

  implicit object PackageJsonFormat extends JsonFormat[Package] {
    def read(json: JsValue): Package = Package(StringJsonFormat.read(json))
    def write(pack: Package): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit val apiGetRecommendationsByCategoryRequestFormat = jsonFormat2(ApiGetRecommendationsByCategoryRequest)

  implicit val apiGetRecommendationsForAppsRequestFormat = jsonFormat4(ApiGetRecommendationsForAppsRequest)

  implicit val googlePlayRecommendationFormat = jsonFormat7(ApiRecommendation)

  implicit val apiGetRecommendationsResponseFormat = jsonFormat1(ApiGetRecommendationsResponse)

  implicit val apiRankAppsByMomentsRequestFormat = jsonFormat4(ApiRankByMomentsRequest)

  implicit val widgetFormat = jsonFormat2(Widget.apply)

  implicit val apiRankedWidgetsByMomentFormat = jsonFormat2(ApiRankedWidgetsByMoment)

  implicit val apiRankWidgetsResponseFormat = jsonFormat1(ApiRankWidgetsResponse)

}

object JsonFormats extends JsonFormats
