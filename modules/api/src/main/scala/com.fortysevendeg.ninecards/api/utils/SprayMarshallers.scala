package com.fortysevendeg.ninecards.api.utils

import cats.data.Xor
import cats.free.Free
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import shapeless.Lazy
import spray.httpx.marshalling.ToResponseMarshaller

import scalaz.concurrent.Task

object SprayMarshallers {

  implicit def catsXorMarshaller[T <: Throwable, A](
    implicit m: ToResponseMarshaller[A]): ToResponseMarshaller[Xor[T, A]] =
    ToResponseMarshaller[Xor[T, A]] {
      (xor, ctx) =>
        xor.fold(
          left => ctx.handleError(left),
          right => m(right, ctx))
    }

  implicit def tasksMarshaller[A](
    implicit m: ToResponseMarshaller[A]): ToResponseMarshaller[Task[A]] =
    ToResponseMarshaller[Task[A]] {
      (task, ctx) =>
        task.runAsync {
          _.fold(
            left => ctx.handleError(left),
            right => m(right, ctx))
        }
    }

  implicit def freeTaskMarshaller[A](
    implicit taskMarshaller: Lazy[ToResponseMarshaller[Task[A]]]): ToResponseMarshaller[Free[NineCardsServices, A]] =
    ToResponseMarshaller[Free[NineCardsServices, A]] {
      (free, ctx) =>
        taskMarshaller.value(free.foldMap(prodInterpreters), ctx)
    }
}
