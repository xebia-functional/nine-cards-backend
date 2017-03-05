/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.api.rankings

import akka.http.scaladsl.marshalling.{ ToEntityMarshaller, ToResponseMarshaller }
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import cards.nine.api.utils.AkkaHttpMarshallers._
import cards.nine.commons.NineCardsService.Result
import cards.nine.processes.NineCardsServices._
import de.heikoseeberger.akkahttpcirce.CirceSupport

import scalaz.concurrent.Task

object NineCardsMarshallers {
  import messages.{ Ranking, Reload }

  private type NineCardsServed[A] = cats.free.Free[NineCardsServices, A]

  implicit lazy val ranking: ToResponseMarshaller[NineCardsServed[Result[Ranking]]] = {
    implicit val resM: ToResponseMarshaller[Ranking] = CirceSupport.circeToEntityMarshaller(Encoders.ranking)
    val resultM: ToResponseMarshaller[Result[Ranking]] = ninecardsResultMarshaller[Ranking]
    val taskM: ToResponseMarshaller[Task[Result[Ranking]]] = tasksMarshaller(resultM)
    freeTaskMarshaller[Result[Ranking]](taskM)
  }

  implicit lazy val reloadResponse: ToResponseMarshaller[NineCardsServed[Result[Reload.Response]]] = {
    implicit val resM: ToResponseMarshaller[Reload.Response] = CirceSupport.circeToEntityMarshaller(Encoders.reloadRankingResponse)
    val resultM: ToResponseMarshaller[Result[Reload.Response]] = ninecardsResultMarshaller[Reload.Response]
    val taskM: ToResponseMarshaller[Task[Result[Reload.Response]]] = tasksMarshaller(resultM)
    freeTaskMarshaller[Result[Reload.Response]](taskM)
  }

  implicit lazy val reloadRequestU: FromEntityUnmarshaller[Reload.Request] =
    CirceSupport.circeUnmarshaller[Reload.Request](
      Decoders.reloadRankingRequest
    )

  implicit lazy val reloadRequesM: ToEntityMarshaller[Reload.Request] =
    CirceSupport.circeToEntityMarshaller(Encoders.reloadRankingRequest)

}
