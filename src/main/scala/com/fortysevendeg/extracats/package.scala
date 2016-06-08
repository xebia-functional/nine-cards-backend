package com.fortysevendeg

import cats.Monad
import cats.data.Xor
import scalaz.concurrent.Task

package object extracats {

  implicit val taskMonad: Monad[Task] = new Monad[Task] {
    override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] = fa.flatMap(f)
    override def pure[A](a: A): Task[A] = Task.now(a)
  }

  class XorTaskOrComposer1[A,E,B](
    one: (A => Task[Xor[E,B]]),
    two: (A => Task[Xor[E,B]])
  ) extends (A => Task[Xor[E,B]]) {

    def apply(a: A) : Task[Xor[E,B]] = {
      // Currently this results in twoR being called twice
      // if oneR fails and twoR returns Xor.left
      val oneR: Task[Xor[E,B]] = one(a)
      lazy val twoR: Task[Xor[E,B]] = two(a)
      val orR = oneR.or(twoR)
      orR.flatMap {
        case Xor.Left(_) => twoR
        case r @ Xor.Right(_) => Task.now(r)
      }
    }
  }

  class XorTaskOrComposer2[A1,A2,E,B](
    first: (A1,A2) => Task[Xor[E,B]],
    secon: (A1,A2) => Task[Xor[E,B]]
  ) extends ( (A1,A2) => Task[Xor[E,B]] ) {

    def apply(a1: A1, a2: A2): Task[Xor[E,B]] = {
      // Currently this results in twoR being called twice
      // if oneR fails and twoR returns Xor.left
      val firstR: Task[Xor[E,B]] = first(a1,a2)
      lazy val seconR: Task[Xor[E,B]] = secon(a1,a2)
      val orR = firstR.or(seconR)
      orR.flatMap {
        case Xor.Left(_) => seconR
        case r @ Xor.Right(_) => Task.now(r)
      }
    }

  }
}
