package com.fortysevendeg.ninecards.processes.utils

import cats.data.Xor
import doobie.imports._
import scalaz.syntax.applicative._

object XorCIO {

  // TODO: Alternative XorF[F[_], A, B] = F[Xor[A,B]]
  // TODO: alternative cats.data.XorT
  type XorCIO[L, R] = ConnectionIO[Xor[L, R]]

  def flatMapXorCIO[L, A, B](xorCIO: XorCIO[L, A], fun: A ⇒ ConnectionIO[B]): XorCIO[L, B] =
    // TODO: cats.data.XorT.liftT, with 
    xorCIO flatMap {
      case left @ Xor.Left(a) ⇒ (left: Xor[L, B]).point[ConnectionIO]
      case Xor.Right(b) ⇒ fun(b) map (Xor.Right.apply)
    }

  def liftXorCIO[L, A, B](fun: A ⇒ ConnectionIO[B]): (A ⇒ XorCIO[L, B]) =
    fun andThen (_.map(Xor.Right.apply))

}