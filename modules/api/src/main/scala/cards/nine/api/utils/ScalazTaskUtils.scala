package cards.nine.api.utils

import scala.concurrent.{ Future, Promise }
import scalaz.{ -\/, \/, \/- }
import scalaz.concurrent.Task

private[api] object ScalazTaskUtils {

  implicit class ScalazTaskFutureOps[A](task: Task[A]) {

    def unsafePerformAsyncFuture(): Future[A] = {
      val p: Promise[A] = Promise()
      task.unsafePerformAsync {
        case \/-(x) ⇒ p.success(x)
        case -\/(error) ⇒ p.failure(error)
      }
      p.future
    }

    def unsafePerformAsyncFutureDisjunction(): Future[Throwable \/ A] = {
      val p: Promise[Throwable \/ A] = Promise()
      task.unsafePerformAsync(x ⇒ p.success(x))
      p.future
    }
  }

}
