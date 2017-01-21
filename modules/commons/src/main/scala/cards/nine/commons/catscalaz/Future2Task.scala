package cards.nine.commons.catscalaz

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }
import scalaz.concurrent.Task
import scalaz.{ \/-, -\/ }

object ScalaFuture2Task {

  def apply[T](fut: Future[T])(implicit ec: ExecutionContext): Task[T] =
    Task.async {
      register ⇒
        fut.onComplete {
          case Success(v) ⇒ register(\/-(v))
          case Failure(ex) ⇒ register(-\/(ex))
        }
    }

}

