package com.fortysevendeg.ninecards.api

import cats.free.Free
import com.fortysevendeg.ninecards.api.messages._
import com.fortysevendeg.ninecards.api.utils.SprayMarshallers._
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import io.circe.spray.JsonSupport._
import io.circe.spray.{ RootDecoder, JsonSupport }
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

  implicit lazy val reloadResponse: ToResponseMarshaller[NineCardsServed[rankings.Reload.XorResponse]] = {
    import rankings.Reload._
    val resM: ToResponseMarshaller[Response] = JsonSupport.circeJsonMarshaller(Encoders.reloadRankingResponse)
    val xorM: ToResponseMarshaller[XorResponse] = catsXorMarshaller[Error, Response](resM)
    val taskM: ToResponseMarshaller[Task[XorResponse]] = tasksMarshaller(xorM)
    freeTaskMarshaller[XorResponse](taskM)
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
