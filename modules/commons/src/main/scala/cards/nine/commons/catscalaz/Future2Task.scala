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
