package cards.nine.api

import cards.nine.api.messages._
import cards.nine.api.utils.SprayMarshallers._
import cards.nine.commons.NineCardsService.Result
import cards.nine.processes.NineCardsServices._
import io.circe.spray.{ JsonSupport, RootDecoder }

import scalaz.concurrent.Task
import spray.httpx.marshalling.{ Marshaller, ToResponseMarshaller }
import spray.httpx.unmarshalling.Unmarshaller

object NineCardsMarshallers {

  private type NineCardsServed[A] = cats.free.Free[NineCardsServices, A]

  implicit lazy val ranking: ToResponseMarshaller[NineCardsServed[rankings.Ranking]] = {
    val resM: ToResponseMarshaller[rankings.Ranking] = JsonSupport.circeJsonMarshaller(Encoders.ranking)
    val taskM: ToResponseMarshaller[Task[rankings.Ranking]] = tasksMarshaller(resM)
    freeTaskMarshaller[rankings.Ranking](taskM)
  }

  implicit lazy val reloadResponse: ToResponseMarshaller[NineCardsServed[Result[rankings.Reload.Response]]] = {
    import rankings.Reload._
    implicit val resM: ToResponseMarshaller[Response] = JsonSupport.circeJsonMarshaller(Encoders.reloadRankingResponse)
    val resultM: ToResponseMarshaller[Result[Response]] = ninecardsResultMarshaller[Response]
    val taskM: ToResponseMarshaller[Task[Result[Response]]] = tasksMarshaller(resultM)
    freeTaskMarshaller[Result[Response]](taskM)
  }

  implicit lazy val reloadRequestU: Unmarshaller[rankings.Reload.Request] = {
    JsonSupport.circeJsonUnmarshaller[rankings.Reload.Request](
      RootDecoder(Decoders.reloadRankingRequest)
    )
  }

  implicit lazy val reloadRequesM: Marshaller[rankings.Reload.Request] = {
    JsonSupport.circeJsonMarshaller(Encoders.reloadRankingRequest)
  }

}
