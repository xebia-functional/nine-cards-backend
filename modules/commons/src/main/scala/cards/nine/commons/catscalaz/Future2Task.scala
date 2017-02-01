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
//
// Source code from Pavel Chlupacek in http://stackoverflow.com/a/17377768/1002111
// published under CC-SA license.
//
// Modified to use the Scalaz std library
//
package cards.nine.commons.catscalaz

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.concurrent.Task
import scalaz.std.{ `try` ⇒ TryF }

object ScalaFuture2Task {

  def apply[T](fut: Future[T])(implicit ec: ExecutionContext): Task[T] =
    Task.async { cont ⇒
      fut.onComplete(x ⇒ cont(TryF.toDisjunction(x)))
    }

}
