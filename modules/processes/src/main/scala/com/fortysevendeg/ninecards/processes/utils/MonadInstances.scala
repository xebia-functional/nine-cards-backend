package com.fortysevendeg.ninecards.processes.utils

import cats.Monad
import doobie.imports._

import scalaz._
import Scalaz._

object MonadInstances {

  implicit val catsMonadCIO = new Monad[ConnectionIO] {
    override def flatMap[A, B](fa: ConnectionIO[A])(f: (A) ⇒ ConnectionIO[B]): ConnectionIO[B] =
      fa.flatMap(f)

    override def pure[A](x: A): ConnectionIO[A] = x.point[ConnectionIO]
  }

}
