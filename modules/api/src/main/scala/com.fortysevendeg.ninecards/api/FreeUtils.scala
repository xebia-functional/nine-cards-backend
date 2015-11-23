package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.processes.NineCardsServices
import spray.httpx.marshalling.ToResponseMarshaller

import scalaz._

object FreeUtils {

  implicit val interpreters = NineCardsServices.interpreters

  implicit def runProcess[S[_], M[_], A](
    sa: Free.FreeC[S, A])(implicit int: S ~> M, M: Monad[M]): M[A] = Free.runFC(sa)(int)

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
