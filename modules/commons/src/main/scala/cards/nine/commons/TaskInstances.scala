package cards.nine.commons

import cats.{ Applicative, ApplicativeError, Monad, RecursiveTailRecM }

import scalaz.concurrent.Task

trait TaskInstances {

  implicit val taskApplicative: Applicative[Task] = ScalazInstances[Task].applicativeInstance

  implicit val taskMonad: Monad[Task] with RecursiveTailRecM[Task] with ApplicativeError[Task, Throwable] =
    new Monad[Task] with RecursiveTailRecM[Task] with ApplicativeError[Task, Throwable] {

      val monadInstance = ScalazInstances[Task].monadInstance

      override def raiseError[A](e: Throwable): Task[A] = Task.fail(e)

      override def handleErrorWith[A](fa: Task[A])(f: (Throwable) ⇒ Task[A]): Task[A] =
        fa.handleWith({ case x ⇒ f(x) })

      override def flatMap[A, B](fa: Task[A])(f: (A) ⇒ Task[B]): Task[B] = monadInstance.flatMap(fa)(f)

      override def tailRecM[A, B](a: A)(f: (A) ⇒ Task[Either[A, B]]): Task[B] = monadInstance.tailRecM(a)(f)

      override def pure[A](x: A): Task[A] = monadInstance.pure(x)
    }
}

object TaskInstances extends TaskInstances