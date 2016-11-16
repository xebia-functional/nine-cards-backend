package cards.nine.commons

import cats.{ Applicative, Monad }

class ScalazInstances[F[_]](implicit A: scalaz.Applicative[F], M: scalaz.Monad[F]) {

  val applicativeInstance: Applicative[F] = new Applicative[F] {

    override def pure[A](x: A): F[A] = A.pure(x)

    override def ap[A, B](ff: F[(A) ⇒ B])(fa: F[A]): F[B] = A.ap(fa)(ff)
  }

  val monadInstance: Monad[F] = new Monad[F] {

    override def pure[A](x: A): F[A] = M.pure(x)

    override def flatMap[A, B](fa: F[A])(f: (A) ⇒ F[B]): F[B] = M.bind(fa)(f)

    override def tailRecM[A, B](a: A)(f: (A) ⇒ F[Either[A, B]]): F[B] = defaultTailRecM(a)(f)
  }
}

object ScalazInstances {
  def apply[F[_]](implicit A: scalaz.Applicative[F], M: scalaz.Monad[F]): ScalazInstances[F] =
    new ScalazInstances[F]
}
