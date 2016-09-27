package cards.nine.processes

import cards.nine.processes.NineCardsServices._
import cards.nine.processes.utils.DatabaseContext._
import cards.nine.services.free.interpreter.Interpreters
import cats._

import scala.util.{ Failure, Success, Try }
import scalaz.concurrent.Task

trait IdInstances {
  implicit def idApplicativeError(
    implicit
    I: Applicative[cats.Id]
  ): ApplicativeError[cats.Id, Throwable] =
    new ApplicativeError[Id, Throwable] {

      override def pure[A](x: A): Id[A] = I.pure(x)

      override def ap[A, B](ff: Id[A ⇒ B])(fa: Id[A]): Id[B] = I.ap(ff)(fa)

      override def map[A, B](fa: Id[A])(f: Id[A ⇒ B]): Id[B] = I.map(fa)(f)

      override def product[A, B](fa: Id[A], fb: Id[B]): Id[(A, B)] = I.product(fa, fb)

      override def raiseError[A](e: Throwable): Id[A] = throw e

      override def handleErrorWith[A](fa: Id[A])(f: Throwable ⇒ Id[A]): Id[A] =
        Try(fa) match {
          case Success(v) ⇒ v
          case Failure(e) ⇒ f(e)
        }
    }
}

trait TestInterpreters extends IdInstances {

  val idInterpreters = new Interpreters[Id] {
    override val task2M: (Task ~> Id) = new (Task ~> Id) {
      override def apply[A](fa: Task[A]): Id[A] = fa.unsafePerformSyncAttempt.fold(
        error ⇒ idApplicativeError.raiseError(error),
        value ⇒ idApplicativeError.pure(value)
      )
    }
  }
  val testNineCardsInterpreters = new NineCardsInterpreters[Id](idInterpreters)

  val testInterpreters: NineCardsServices ~> Id = testNineCardsInterpreters.interpreters
}
