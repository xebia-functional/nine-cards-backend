package cards.nine.googleplay.service.free.interpreter.googleapi

import cards.nine.googleplay.domain._
import cats.data.Xor
import scalaz.concurrent.Task

case class ApiServices(apiClient: ApiClient, appService: AppRequest ⇒ Task[InfoError Xor FullCard]) {

  def getItem(request: AppRequest): Task[Xor[String, Item]] =
    apiClient.details(request.packageName, request.authParams).map { xor ⇒
      xor.bimap(
        _res ⇒ request.packageName.value,
        docV2 ⇒ Converters.toItem(docV2)
      )
    }

}
