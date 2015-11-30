package com.fortysevendeg.ninecards.api

import cats.free.Free
import cats.{Monad, ~>}
import com.fortysevendeg.ninecards.processes.NineCardsServices
import spray.httpx.marshalling.ToResponseMarshaller

import scala.language.{higherKinds, implicitConversions}
import scalaz.concurrent.Task

object FreeUtils {

  implicit val interpreters = NineCardsServices.interpreters

  implicit def taskMonad = new Monad[Task] {
    override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] =
      fa.flatMap(f)

    override def pure[A](a: A): Task[A] = Task.now(a)
  }

  implicit def runProcess[S[_], M[_], A](
    sa: Free[S, A])(implicit int: S ~> M, M: Monad[M]): M[A] = sa.foldMap(int)

  implicit def tasksMarshaller[A](
    implicit m: ToResponseMarshaller[A]): ToResponseMarshaller[scalaz.concurrent.Task[A]] =
    ToResponseMarshaller[scalaz.concurrent.Task[A]] {
      (task, ctx) =>
        task.runAsync { _.fold(
          left => ctx.handleError(left),
          right => m(right, ctx))
        }
    }
}
