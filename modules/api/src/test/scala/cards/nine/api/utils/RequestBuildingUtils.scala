package cards.nine.api.utils

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.client.RequestBuilding.RequestTransformer
import cats.data.NonEmptyList

trait RequestBuildingUtils {

  def addHeaders(headers: NonEmptyList[HttpHeader]): RequestTransformer =
    _.mapHeaders(_ ++ headers.toList)

}
