package com.fortysevendeg.ninecards.api.utils

import cats.free.Free
import cats.{Monad, ~>}

object FreeUtils {

  implicit def runProcess[S[_], M[_], A](
    sa: Free[S, A])(implicit int: S ~> M, M: Monad[M]): M[A] = sa.foldMap(int)
}
