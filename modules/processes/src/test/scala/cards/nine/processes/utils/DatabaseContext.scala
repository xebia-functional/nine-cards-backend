package cards.nine.processes.utils

import cards.nine.services.persistence.DatabaseTransactor
import cats.Id
import doobie.imports.Transactor
import doobie.util.capture.Capture

import scalaz.{ Catchable, Monad, \/ }

trait CatsIdInstances {

  implicit val idMonad = new Monad[Id] {
    override def bind[A, B](fa: Id[A])(f: (A) ⇒ Id[B]): Id[B] = f(fa)

    override def point[A](a: ⇒ A): Id[A] = a
  }

  implicit val idCatchable = new Catchable[Id] {

    override def attempt[A](f: Id[A]): Id[\/[Throwable, A]] = \/.fromTryCatchNonFatal(f)

    override def fail[A](err: Throwable): Id[A] = throw err
  }

  implicit val idCapture = new Capture[Id] {
    override def apply[A](a: ⇒ A): Id[A] = a
  }
}

object DatabaseContext extends DummyNineCardsConfig with CatsIdInstances {

  implicit val transactor: Transactor[Id] = new DatabaseTransactor(config).transactor
}
