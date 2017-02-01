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
package cards.nine.api.utils

import cards.nine.api.NineCardsErrorHandler
import cards.nine.commons.NineCardsService.{ NineCardsService, Result }
import cards.nine.processes.NineCardsServices._
import cats.free.Free
import shapeless.Lazy
import spray.httpx.marshalling.ToResponseMarshaller

import scalaz.concurrent.Task

object SprayMarshallers {

  implicit def nineCardsServiceMarshaller[A](
    implicit
    m: ToResponseMarshaller[Free[NineCardsServices, Result[A]]]
  ): ToResponseMarshaller[NineCardsService[NineCardsServices, A]] =
    ToResponseMarshaller[NineCardsService[NineCardsServices, A]] {
      (result, ctx) ⇒
        m(result.value, ctx)
    }

  implicit def ninecardsResultMarshaller[A](
    implicit
    m: ToResponseMarshaller[A],
    handler: NineCardsErrorHandler
  ): ToResponseMarshaller[Result[A]] =
    ToResponseMarshaller[Result[A]] {
      (result, ctx) ⇒
        result.fold(
          left ⇒ handler.handleNineCardsErrors(left, ctx),
          right ⇒ m(right, ctx)
        )
    }

  implicit def tasksMarshaller[A](
    implicit
    m: ToResponseMarshaller[A]
  ): ToResponseMarshaller[Task[A]] =
    ToResponseMarshaller[Task[A]] {
      (task, ctx) ⇒
        task.unsafePerformAsync {
          _.fold(
            left ⇒ ctx.handleError(left),
            right ⇒ m(right, ctx)
          )
        }
    }

  implicit def freeTaskMarshaller[A](
    implicit
    taskMarshaller: Lazy[ToResponseMarshaller[Task[A]]]
  ): ToResponseMarshaller[Free[NineCardsServices, A]] =
    ToResponseMarshaller[Free[NineCardsServices, A]] {
      (free, ctx) ⇒
        taskMarshaller.value(free.foldMap(prodInterpreters), ctx)
    }
}
