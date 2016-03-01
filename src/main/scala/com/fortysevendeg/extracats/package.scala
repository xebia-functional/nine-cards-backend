package com.fortysevendeg

import cats.Monad
import scalaz.concurrent.Task

package object extracats {
  implicit def taskInstance = new Monad[Task] {
    override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] = fa.flatMap(f)
    override def pure[A](a: A): Task[A] = Task.now(a)
  }
}
