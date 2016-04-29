package com.fortysevendeg.ninecards.services.free.interpreter

import cats._
import com.fortysevendeg.ninecards.services.free.algebra.DBResult._
import com.fortysevendeg.ninecards.services.free.algebra.GoogleApiServices._
import com.fortysevendeg.ninecards.services.free.interpreter.impl.GoogleApiServices._

import scala.util.{ Failure, Success, Try }
import scalaz.concurrent.Task

abstract class Interpreters[M[_]](implicit A: ApplicativeError[M, Throwable]) {

  val task2M: (Task ~> M)

  def dBResultInterpreter: (DBResult ~> M) = new (DBResult ~> M) {
    def apply[A](fa: DBResult[A]) = fa match {
      case DBSuccess(value) ⇒ A.pureEval(Eval.later(value))
      case DBFailure(e) ⇒ A.raiseError(e)
    }
  }

  def googleAPIServicesInterpreter: (GoogleApiOps ~> M) = new (GoogleApiOps ~> M) {
    def apply[A](fa: GoogleApiOps[A]) = fa match {
      case GetTokenInfo(tokenId: String) ⇒ task2M(googleApiServices.getTokenInfo(tokenId))
    }
  }
}

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

trait TaskInstances {
  implicit val taskMonad: Monad[Task] with ApplicativeError[Task, Throwable] =
    new Monad[Task] with ApplicativeError[Task, Throwable] {

      def pure[A](x: A): Task[A] = Task.delay(x)

      override def map[A, B](fa: Task[A])(f: A ⇒ B): Task[B] =
        fa map f

      override def flatMap[A, B](fa: Task[A])(f: A ⇒ Task[B]): Task[B] =
        fa flatMap f

      override def pureEval[A](x: Eval[A]): Task[A] =
        Task.fork(Task.delay(x.value))

      override def raiseError[A](e: Throwable): Task[A] =
        Task.fail(e)

      override def handleErrorWith[A](fa: Task[A])(f: Throwable ⇒ Task[A]): Task[A] =
        fa.handleWith({ case x ⇒ f(x) })
    }
}

object Interpreters extends IdInstances with TaskInstances {

  val taskInterpreters = new Interpreters[Task] {
    override val task2M: (Task ~> Task) = new (Task ~> Task) {
      override def apply[A](fa: Task[A]): Task[A] = fa
    }
  }

  val idInterpreters = new Interpreters[Id] {
    override val task2M: (Task ~> Id) = new (Task ~> Id) {
      override def apply[A](fa: Task[A]): Id[A] = fa.attemptRun.fold(
        error ⇒ idApplicativeError.raiseError(error),
        value ⇒ idApplicativeError.pure(value)
      )
    }
  }
}
