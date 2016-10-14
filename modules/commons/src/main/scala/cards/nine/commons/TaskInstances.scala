package cards.nine.commons

import cats.{ ApplicativeError, Monad, RecursiveTailRecM }

import scalaz.concurrent.Task

trait TaskInstances {

  implicit val taskMonad: Monad[Task] with ApplicativeError[Task, Throwable] with RecursiveTailRecM[Task] =
    new Monad[Task] with ApplicativeError[Task, Throwable] with RecursiveTailRecM[Task] {

      def pure[A](x: A): Task[A] = Task.delay(x)

      override def map[A, B](fa: Task[A])(f: A ⇒ B): Task[B] =
        fa map f

      override def flatMap[A, B](fa: Task[A])(f: A ⇒ Task[B]): Task[B] =
        fa flatMap f

      override def raiseError[A](e: Throwable): Task[A] =
        Task.fail(e)

      override def handleErrorWith[A](fa: Task[A])(f: Throwable ⇒ Task[A]): Task[A] =
        fa.handleWith({ case x ⇒ f(x) })

      override def tailRecM[A, B](a: A)(f: (A) ⇒ Task[Either[A, B]]): Task[B] =
        defaultTailRecM(a)(f)
    }
}

object TaskInstances extends TaskInstances
