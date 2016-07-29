package com.fortysevendeg.ninecards.processes.utils

import cats.data.XorT
import cats.{ Functor, Unapply }

object XorTSyntax extends XorTSyntax1 {
  implicit def catsSyntaxXorT[F[_]: Functor, A](fa: F[A]): XorTFunctorOps[F, A] = new XorTFunctorOps(fa)
}

trait XorTSyntax1 {
  implicit def catsSyntaxUXorT[FA](fa: FA)(implicit U: Unapply[Functor, FA]): XorTFunctorOps[U.M, U.A] =
    new XorTFunctorOps[U.M, U.A](U.subst(fa))(U.TC)
}

final class XorTFunctorOps[F[_]: Functor, A](val fa: F[A]) {

  def leftXorT[R]: XorT[F, A, R] = XorT.left[F, A, R](fa)

  def rightXorT[L]: XorT[F, L, A] = XorT.right[F, L, A](fa)
}