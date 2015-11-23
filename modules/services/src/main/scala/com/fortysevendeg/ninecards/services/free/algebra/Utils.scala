package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}

import scala.language.higherKinds

object Utils {

  def lift[F[_], G[_], A](fa: F[A])(implicit I: Inject[F, G]): Free[G, A] = Free.liftF(I.inj(fa))

}
