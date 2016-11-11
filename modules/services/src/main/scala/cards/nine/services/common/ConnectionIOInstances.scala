package cards.nine.services.common

import cats.{ Applicative, Monad }
import doobie.imports._

object ConnectionIOInstances {

  implicit val connectionIOApplicative: Applicative[ConnectionIO] = new Applicative[ConnectionIO] {

    override def pure[A](x: A): ConnectionIO[A] = scalaz.Applicative[ConnectionIO].pure(x)

    override def ap[A, B](ff: ConnectionIO[(A) ⇒ B])(fa: ConnectionIO[A]): ConnectionIO[B] =
      scalaz.Applicative[ConnectionIO].ap(fa)(ff)
  }

  implicit val connectionIOMonad: Monad[ConnectionIO] = new Monad[ConnectionIO] {

    override def pure[A](x: A): ConnectionIO[A] = scalaz.Applicative[ConnectionIO].pure(x)

    override def flatMap[A, B](fa: ConnectionIO[A])(f: (A) ⇒ ConnectionIO[B]): ConnectionIO[B] =
      fa.flatMap(f)

    override def tailRecM[A, B](a: A)(f: (A) ⇒ ConnectionIO[Either[A, B]]): ConnectionIO[B] =
      defaultTailRecM(a)(f)
  }
}
