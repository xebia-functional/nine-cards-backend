package com.fortysevendeg.ninecards.api.utils

import cats.free.Free
import cats.{Monad, ~>}
import com.fortysevendeg.ninecards.processes.NineCardsServices

import scala.language.{higherKinds, implicitConversions}

object FreeUtils {

  implicit val interpreters = NineCardsServices.interpreters

  implicit def runProcess[S[_], M[_], A](
    sa: Free[S, A])(implicit int: S ~> M, M: Monad[M]): M[A] = sa.foldMap(int)


  //  implicit def freeMarshaller[S[_], M[_], A](
  //    implicit int: S ~> M,
  //    monadMarshaller: Lazy[ToResponseMarshaller[M[A]]],
  //    monad: Monad[M]): ToResponseMarshaller[Free[S, A]] = ToResponseMarshaller[Free[S, A]] {
  //    (free, ctx) =>
  //      monadMarshaller.value(free.foldMap(int), ctx)
  //  }
}
