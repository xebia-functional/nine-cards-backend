package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.processes.NineCardsServices

import scalaz._

object FreeUtils {

  implicit val interpreters = NineCardsServices.interpreters

  implicit def runProcess[S[_], M[_], A](sa: Free.FreeC[S, A])(implicit int: S ~> M, M: Monad[M]): M[A] = {
    Free.runFC(sa)(int)
  }
}
