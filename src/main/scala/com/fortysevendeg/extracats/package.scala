package com.fortysevendeg

import cats.Monad
import cats.data.Xor
import scalaz.concurrent.Task

package object extracats {

  implicit val taskMonad: Monad[Task] = new Monad[Task] {
    override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] = fa.flatMap(f)
    override def pure[A](a: A): Task[A] = Task.now(a)
  }

  class XorTaskOrComposer[A,E,B](
    leftFunction: (A => Task[Xor[E,B]]),
    rightFunction: (A => Task[Xor[E,B]])
  ) extends (A => Task[Xor[E,B]]) {

    def apply(a: A) : Task[Xor[E,B]] = {
      // Currently this results in twoR being called twice
      // if oneR fails and twoR returns Xor.left
      val leftResult: Task[Xor[E,B]] = leftFunction(a)
      lazy val rightResult: Task[Xor[E,B]] = rightFunction(a)
      val orResult = leftResult.or(rightResult)
      orResult.flatMap {
        case Xor.Left(_) => rightResult
        case res => Task.now(res)
      }
    }
  }

}
