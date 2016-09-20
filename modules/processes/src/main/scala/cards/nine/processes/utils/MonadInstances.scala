package cards.nine.processes.utils

import cats.Monad
import doobie.imports._

object MonadInstances {

  implicit def catsMonadCIO(implicit ZM: scalaz.Monad[ConnectionIO]) = new Monad[ConnectionIO] {
    override def flatMap[A, B](fa: ConnectionIO[A])(f: (A) ⇒ ConnectionIO[B]): ConnectionIO[B] =
      ZM.bind(fa)(f)

    override def pure[A](x: A): ConnectionIO[A] = ZM.point(x)
  }

}
