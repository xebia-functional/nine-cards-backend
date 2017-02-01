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

import shapeless.{ ::, HList, HNil }
import spray.httpx.marshalling.ToResponseMarshaller
import spray.routing.{ Directive, HListable, _ }

import scala.util.control.NonFatal
import scalaz.concurrent.Task
import scalaz.{ -\/, \/, \/- }

trait TaskDirectives {

  /**
    * "Unwraps" a ``Task[T]`` and runs its inner route after future
    * completion with the task's value as an extraction of type ``Throwable \/ T``.
    */
  def onComplete[T](magnet: OnCompleteTaskMagnet[T]): Directive1[Throwable \/ T] = magnet

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
  def onFailure(magnet: OnFailureTaskMagnet): Directive1[Throwable] = magnet
}

object TaskDirectives extends TaskDirectives

trait OnCompleteTaskMagnet[T] extends Directive1[Throwable \/ T]

object OnCompleteTaskMagnet {
  implicit def apply[T](task: ⇒ Task[T]): OnCompleteTaskMagnet[T] =
    new OnCompleteTaskMagnet[T] {
      def happly(f: ((Throwable \/ T) :: HNil) ⇒ Route): Route = ctx ⇒
        task.unsafePerformAsync { t ⇒
          try f(t :: HNil)(ctx)
          catch { case NonFatal(error) ⇒ ctx.failWith(error) }
        }
    }
}

trait OnSuccessTaskMagnet {
  type Out <: HList

  def directive: Directive[Out]
}

object OnSuccessTaskMagnet {
  import scala.language.existentials
  implicit def apply[T](task: ⇒ Task[T])(implicit hl: HListable[T]) =
    new Directive[hl.Out] with OnSuccessTaskMagnet {
      type Out = hl.Out

      def directive = this

      def happly(f: Out ⇒ Route) = ctx ⇒ task.unsafePerformAsync {

        case \/-(t) ⇒
          try f(hl(t))(ctx)
          catch {
            case NonFatal(error) ⇒ ctx.failWith(error)
          }
        case -\/(error) ⇒ ctx.failWith(error)
      }
    }
}

trait OnFailureTaskMagnet extends Directive1[Throwable]

object OnFailureTaskMagnet {
  implicit def apply[T](task: ⇒ Task[T])(implicit m: ToResponseMarshaller[T]) =
    new OnFailureTaskMagnet {
      def happly(f: (Throwable :: HNil) ⇒ Route) = ctx ⇒ task.unsafePerformAsync {
        case \/-(t) ⇒ ctx.complete(t)
        case -\/(error) ⇒
          try f(error :: HNil)(ctx)
          catch {
            case NonFatal(err) ⇒ ctx.failWith(err)
          }
      }
    }
}
