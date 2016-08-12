package cards.nine.services.common

import cats.data.{ Xor, XorT }
import cards.nine.services.free.algebra.DBResult.DBOps
import cards.nine.services.persistence.PersistenceExceptions.PersistenceException
import doobie.contrib.hikari.hikaritransactor.HikariTransactor
import doobie.imports.ConnectionIO

import scalaz.concurrent.Task
import scalaz.{ -\/, \/- }

object ConnectionIOOps {

  implicit val functorCIO = new cats.Functor[ConnectionIO] {
    override def map[A, B](fa: ConnectionIO[A])(f: (A) ⇒ B): ConnectionIO[B] = fa.map(f)
  }

  implicit class ConnectionIOOps[A](c: ConnectionIO[A]) {
    def liftF[F[_]](implicit dbOps: DBOps[F], transactor: Task[HikariTransactor[Task]]): cats.free.Free[F, A] =
      transactor.flatMap(_.trans(c)).unsafePerformSyncAttempt match {
        case \/-(value) ⇒ dbOps.success(value)
        case -\/(e) ⇒
          dbOps.failure(
            PersistenceException(
              message = "An error was found while accessing to database",
              cause   = Option(e)
            )
          )
      }
  }

  implicit class CatsFreeOps[F[_], A, T](c: cats.free.Free[F, T Xor A]) {
    def toXorT = XorT[cats.free.Free[F, ?], T, A](c)
  }

  implicit class ScalazFreeOps[F[_], A, T](c: scalaz.Free[F, T Xor A]) {
    def toXorT = XorT[scalaz.Free[F, ?], T, A](c)
  }

  implicit class TaskOps[A](task: Task[A]) {
    def liftF[F[_]](implicit dbOps: DBOps[F]): cats.free.Free[F, A] =
      task.unsafePerformSyncAttempt match {
        case \/-(value) ⇒ dbOps.success(value)
        case -\/(e) ⇒
          dbOps.failure(
            PersistenceException(
              message = "An error was found while accessing to database",
              cause   = Option(e)
            )
          )
      }
  }

}
