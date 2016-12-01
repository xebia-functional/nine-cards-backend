package cards.nine.api.rankings

import cards.nine.api.utils.SprayMarshallers._
import cards.nine.commons.NineCardsService.Result
import cards.nine.processes.NineCardsServices._
import io.circe.spray.{ JsonSupport, RootDecoder }
import scalaz.concurrent.Task
import spray.httpx.marshalling.{ Marshaller, ToResponseMarshaller }
import spray.httpx.unmarshalling.Unmarshaller

object NineCardsMarshallers {
  import messages.{ Ranking, Reload }

  private type NineCardsServed[A] = cats.free.Free[NineCardsServices, A]

  implicit lazy val ranking: ToResponseMarshaller[NineCardsServed[Result[Ranking]]] = {
    implicit val resM: ToResponseMarshaller[Ranking] = JsonSupport.circeJsonMarshaller(Encoders.ranking)
    val resultM: ToResponseMarshaller[Result[Ranking]] = ninecardsResultMarshaller[Ranking]
    val taskM: ToResponseMarshaller[Task[Result[Ranking]]] = tasksMarshaller(resultM)
    freeTaskMarshaller[Result[Ranking]](taskM)
  }

  implicit lazy val reloadResponse: ToResponseMarshaller[NineCardsServed[Result[Reload.Response]]] = {
    implicit val resM: ToResponseMarshaller[Reload.Response] = JsonSupport.circeJsonMarshaller(Encoders.reloadRankingResponse)
    val resultM: ToResponseMarshaller[Result[Reload.Response]] = ninecardsResultMarshaller[Reload.Response]
    val taskM: ToResponseMarshaller[Task[Result[Reload.Response]]] = tasksMarshaller(resultM)
    freeTaskMarshaller[Result[Reload.Response]](taskM)
  }

  implicit lazy val reloadRequestU: Unmarshaller[Reload.Request] =
    JsonSupport.circeJsonUnmarshaller[Reload.Request](
      RootDecoder(Decoders.reloadRankingRequest)
    )

  implicit lazy val reloadRequesM: Marshaller[Reload.Request] =
    JsonSupport.circeJsonMarshaller(Encoders.reloadRankingRequest)

}
