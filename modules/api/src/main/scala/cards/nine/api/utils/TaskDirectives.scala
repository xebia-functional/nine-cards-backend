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

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.server.util.Tupler
import akka.http.scaladsl.server.{ Directive, Directive1 }

import scalaz.concurrent.Task
import scalaz.{ -\/, \/, \/- }

import ScalazTaskUtils._

trait TaskDirectives {

  /**
    * "Unwraps" a ``Task[T]`` and runs its inner route after future
    * completion with the task's value as an extraction of type ``Throwable \/ T``.
    */
  def onComplete[T](task: ⇒ Task[T]): Directive1[Throwable \/ T] =
    Directive { inner ⇒ ctx ⇒
      import ctx.executionContext
      task.unsafePerformAsyncFutureDisjunction().flatMap(t ⇒ inner(Tuple1(t))(ctx))
    }

  /**
    * "Unwraps" a ``Task[T]`` and runs its inner route after future
    * completion with the task's value as an extraction of type ``T``.
    * If the task fails its failure throwable is bubbled up to the nearest
    * ExceptionHandler.
    * If type ``T`` is already an HList it is directly expanded into the respective
    * number of extractions.
    */
  def onSuccess(magnet: OnSuccessTaskMagnet): Directive[magnet.Out] = magnet.directive

  /**
    * "Unwraps" a ``Task[T]`` and runs its inner route when the task has failed
    * with the task's failure exception as an extraction of type ``Throwable``.
    * If the task succeeds the request is completed using the values marshaller
    * (This directive therefore requires a marshaller for the task type to be
    * implicitly available.)
    */
  def completeOrRecoverWith(magnet: CompleteOrRecoverWithTaskMagnet): Directive1[Throwable] = magnet.directive
}

object TaskDirectives extends TaskDirectives

trait OnSuccessTaskMagnet {
  type Out
  def directive: Directive[Out]
}

object OnSuccessTaskMagnet {
  implicit def apply[T](task: ⇒ Task[T])(implicit tupler: Tupler[T]) =
    new OnSuccessTaskMagnet {
      type Out = tupler.Out

      val directive = Directive[tupler.Out] { inner ⇒ ctx ⇒
        import ctx.executionContext
        task.unsafePerformAsyncFuture().flatMap(t ⇒ inner(tupler(t))(ctx))
      }(tupler.OutIsTuple)
    }
}

trait CompleteOrRecoverWithTaskMagnet {
  def directive: Directive1[Throwable]
}

object CompleteOrRecoverWithTaskMagnet {
  implicit def apply[T](task: ⇒ Task[T])(implicit m: ToResponseMarshaller[T]): CompleteOrRecoverWithTaskMagnet =
    new CompleteOrRecoverWithTaskMagnet {
      val directive = Directive[Tuple1[Throwable]] { inner ⇒ ctx ⇒
        import ctx.executionContext
        task.unsafePerformAsyncFutureDisjunction().flatMap {
          case \/-(t) ⇒ ctx.complete(t)
          case -\/(error) ⇒ inner(Tuple1(error))(ctx)
        }
      }
    }
}
