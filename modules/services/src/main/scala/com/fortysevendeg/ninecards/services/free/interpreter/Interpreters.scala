package com.fortysevendeg.ninecards.services.free.interpreter

import cats._
import com.fortysevendeg.ninecards.services.free.algebra.DBResult._
import com.fortysevendeg.ninecards.services.free.algebra.GoogleApiServices._
import com.fortysevendeg.ninecards.services.free.interpreter.impl.GoogleApiServices._

import scalaz.concurrent.Task

class Interpreters[M[_]](implicit A: ApplicativeError[M, Throwable]) {

  def dBResultInterpreter: (DBResult ~> M) = new (DBResult ~> M) {
    def apply[A](fa: DBResult[A]) = fa match {
      case DBSuccess(value) => A.pureEval(Eval.later(value))
      case DBFailure(e) => A.raiseError(e)
    }
  }

  def googleAPIServicesInterpreter: (GoogleApiOps ~> Task) = new (GoogleApiOps ~> Task) {

    def apply[A](fa: GoogleApiOps[A]) = fa match {
      case GetTokenInfo(tokenId: String) =>
        googleApiServices.getTokenInfo(tokenId)
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

object Interpreters extends TaskInstances {

  val interpreters = new Interpreters[Task]

}
