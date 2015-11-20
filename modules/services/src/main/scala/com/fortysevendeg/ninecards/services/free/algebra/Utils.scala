package com.fortysevendeg.ninecards.services.free.algebra

import scalaz.Free._
import scalaz.{Free, Inject}

object Utils {

  def lift[F[_], G[_], A](fa: F[A])(implicit I: Inject[F, G]): FreeC[G, A] = Free.liftFC(I.inj(fa))

}
