package cards.nine.services.common

import cards.nine.commons.catscalaz.ScalazInstances
import cards.nine.commons.NineCardsErrors.NineCardsError
import cats.data.EitherT
import cats.syntax.either._
import doobie.imports._

object PersistenceService {
  type PersistenceService[A] = ConnectionIO[NineCardsError Either A]

  def apply[A](connectionIO: ConnectionIO[A]): PersistenceService[A] = connectionIO map Either.right
  def apply[A](value: A): PersistenceService[A] =
    ScalazInstances[ConnectionIO].monadInstance.pure(value) map Either.right

  implicit class PersistenceServiceOps[A](service: PersistenceService[A]) {
    def toEitherT = EitherT[ConnectionIO, NineCardsError, A](service)
  }
}
